(ns webradio.player
  (:require [webradio.model :as model]
            [webradio.metadata :as metadata])
  (:import [javazoom.jl.player.advanced AdvancedPlayer]
           [java.net URL]))

(defonce playing? (atom false)) ;; Flag pour signaler si la radio est en cours de lecture

(defn stop-radio []
  (when @playing?
    (reset! playing? false) ;; Désactiver la lecture
    (when-let [player @model/current-player]
      (.close player)
      (reset! model/current-player nil))
    (metadata/stop-metadata-fetcher))) ;; Arrêter aussi la récupération des métadonnées

(defn play-radio [radio]
  (stop-radio) ;; Arrêter l'ancienne radio proprement
  (try
    (let [url (URL. (:url radio))
          input-stream (.openStream url)
          player (AdvancedPlayer. input-stream)]
      (reset! playing? true)
      (reset! model/current-player player)

      ;; Démarrer la récupération des métadonnées
      (metadata/start-metadata-fetcher (:url radio))

      ;; Démarrer le player dans un thread
      (.start (Thread.
               (fn []
                 (try
                   (.play player)
                   (catch Exception e
                     (println "Erreur de lecture:" (.getMessage e)))
                   (finally
                     (reset! playing? false) ;; Réinitialiser le flag après l'arrêt
                     (reset! model/current-player nil)))))))
    (catch Exception e
      (println "Erreur de lecture:" (.getMessage e))
      (reset! playing? false))))

(comment
  (play-radio {:url "http://direct.franceinter.fr/live/franceinter-midfi.mp3"})
  (Thread/sleep 5000) ;; Attendre un peu
  (stop-radio))
