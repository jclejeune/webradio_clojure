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

(defn fetch-metadata [stream-url]
  (try
    (let [url (URL. stream-url)
          conn (.openConnection url)]
      (.setRequestProperty conn "Icy-MetaData" "1")
      (.connect conn)
      (let [icy-title (.getHeaderField conn "icy-name")
            meta-int (.getHeaderFieldInt conn "icy-metaint" -1)]
        ;; N'afficher le nom de la station que s'il a changé
        (when (and icy-title (not= icy-title @last-station))
          (reset! last-station icy-title)
          (println "Station actuelle:" icy-title))
        (when (pos? meta-int)
          (with-open [stream (BufferedInputStream. (.getInputStream conn))]
            (let [buffer (byte-array meta-int)]
              (.read stream buffer 0 meta-int)
              (let [metadata-length (* 16 (.read stream))]
                (when (pos? metadata-length)
                  (let [metadata-buffer (byte-array metadata-length)]
                    (.read stream metadata-buffer 0 metadata-length)
                    (let [metadata (String. metadata-buffer 0 metadata-length (Charset/forName "UTF-8"))
                          title (extract-title metadata)]
                      (when (and title (not= title @last-title))
                        (reset! last-title title)
                        (println "Titre en cours:" title))
                      (reset! error-count 0))))))))))
    (catch Exception e
      (swap! error-count inc)
      (println "Erreur de récupération des métadonnées:" (.getMessage e))
      (when (>= @error-count max-errors)
        (println "Trop d'erreurs consécutives, arrêt du fetcher.")
        (stop-metadata-fetcher)))))

(defn start-metadata-fetcher [stream-url]
  (stop-metadata-fetcher) ;; Arrêter un éventuel fetcher en cours
  (let [scheduler (Executors/newScheduledThreadPool 1)]
    (reset! metadata-scheduler scheduler)
    (.scheduleAtFixedRate scheduler #(fetch-metadata stream-url) 0 10 TimeUnit/SECONDS)))
