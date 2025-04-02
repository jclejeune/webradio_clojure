(ns webradio.metadata
  (:import [java.net URL]
           [java.io BufferedInputStream]))

(defonce metadata-thread (atom nil)) ;; Pour stocker le thread en cours

(defn extract-title [metadata]
  (when-let [match (re-find #"StreamTitle='([^']+)'" metadata)]
    (second match)))

(defn get-icy-metadata [stream-url]
  (future
    (try
      (let [url (URL. stream-url)
            conn (.openConnection url)]
        (.setRequestProperty conn "Icy-MetaData" "1")
        (.connect conn)

        (let [icy-title (.getHeaderField conn "icy-name")
              meta-int (.getHeaderFieldInt conn "icy-metaint" -1)]
          (println "🎙 Station actuelle" icy-title)

          (if (pos? meta-int)
            (with-open [stream (BufferedInputStream. (.getInputStream conn))]
              (loop []
                (let [buffer (byte-array meta-int)]
                  (.read stream buffer 0 meta-int)
                  (let [metadata-length (* 16 (.read stream))]
                    (when (pos? metadata-length)
                      (let [metadata-buffer (byte-array metadata-length)]
                        (.read stream metadata-buffer 0 metadata-length)
                        (let [metadata (String. (.getBytes (String. metadata-buffer "ISO-8859-1") "ISO-8859-1") "UTF-8")
                              title (extract-title metadata)]
                          (when title
                            (println "🎵 Titre en cours: " title))
                          (Thread/sleep 1000) ;; Plus réactif !
                          (recur))))))))
            (println "⚠️ Aucune métadonnée ICY trouvée."))

          {:station icy-title}))
      (catch Exception e
        (println "❌ Erreur de récupération des métadonnées: " (.getMessage e))
        nil))))


(defn start-metadata-fetcher [stream-url]
  (when @metadata-thread
    (println "🔄 Arrêt du thread précédent...")
    (future-cancel @metadata-thread)) ;; Arrêter le thread précédent proprement

  (reset! metadata-thread
          (future (get-icy-metadata stream-url)))) ;; Lancer un nouveau thread

