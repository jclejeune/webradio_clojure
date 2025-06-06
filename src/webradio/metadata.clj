(ns webradio.metadata
  (:import [java.net URL]
           [java.io BufferedInputStream]
           [java.nio.charset Charset]
           [java.util.concurrent Executors TimeUnit]))

;; Atomes pour stocker l'état du fetcher
(defonce metadata-scheduler (atom nil))
(defonce last-title (atom nil))
(defonce error-count (atom 0))
(def max-errors 5)
(defonce last-station (atom nil))

(defn extract-title [metadata]
  (when-let [match (re-find #"StreamTitle='([^']+)'" metadata)]
    (second match)))

(defn stop-metadata-fetcher []
  (when @metadata-scheduler
    (.shutdown @metadata-scheduler)
    (reset! metadata-scheduler nil)
    ;; (println "Metadata fetcher arrêté.")
    ))

;; Extraction et traitement des métadonnées depuis le flux
(defn- extract-and-process-metadata [stream metadata-length]
  (let [metadata-buffer (byte-array metadata-length)]
    (.read stream metadata-buffer 0 metadata-length)

    (let [metadata (String. metadata-buffer 0 metadata-length (Charset/forName "UTF-8"))
          title (extract-title metadata)]

      ;; Mise à jour du titre si nécessaire
      (when (and title (not= title @last-title))
        (reset! last-title title)
        (println "Titre en cours:" title))

      ;; Réinitialisation du compteur d'erreurs
      (reset! error-count 0))))

;; Fonction pour traiter le flux et extraire les métadonnées
(defn- process-metadata-stream [conn meta-int]
  (with-open [stream (-> conn .getInputStream BufferedInputStream.)]
    ;; Lecture des données audio jusqu'au bloc de métadonnées
    (let [buffer (byte-array meta-int)]
      (.read stream buffer 0 meta-int)

      ;; Lecture de la longueur des métadonnées
      (let [metadata-length (* 16 (.read stream))]
        (when (pos? metadata-length)
          (extract-and-process-metadata stream metadata-length))))))

;; Gestion des erreurs de récupération des métadonnées
(defn- handle-metadata-error [e]
  (swap! error-count inc)
  (println "Erreur de récupération des métadonnées:" (.getMessage e))

  ;; Arrêt du fetcher si trop d'erreurs
  (when (>= @error-count max-errors)
    (println "Trop d'erreurs consécutives, arrêt du fetcher.")
    (stop-metadata-fetcher)))

(defn fetch-metadata [stream-url]
  (try
    (let [conn (-> stream-url
                   URL.
                   .openConnection
                   (doto
                    (.setRequestProperty "Icy-MetaData" "1")
                     .connect))
          icy-title (.getHeaderField conn "icy-name")
          meta-int (.getHeaderFieldInt conn "icy-metaint" -1)]

      ;; Gestion du titre de la station
      (when (and icy-title (not= icy-title @last-station))
        (reset! last-station icy-title)
        (println "Station actuelle:" icy-title))

      ;; Extraction des métadonnées
      (when (pos? meta-int)
        (process-metadata-stream conn meta-int)))

    (catch Exception e
      (handle-metadata-error e))))

(defn start-metadata-fetcher [stream-url]
  (stop-metadata-fetcher) ;; Arrêter un éventuel fetcher en cours
  (let [scheduler (Executors/newScheduledThreadPool 1)]
    (reset! metadata-scheduler scheduler)
    (.scheduleAtFixedRate scheduler #(fetch-metadata stream-url) 0 10 TimeUnit/SECONDS)))