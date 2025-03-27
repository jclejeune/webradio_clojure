(ns webradio.player
  (:require [webradio.model :as model]) ; Correction require
  (:import [javazoom.jl.player.advanced AdvancedPlayer] ; Correction import
           [java.net URL]))

(defn stop-radio []
  (when-let [player @model/current-player]
    (.close player)
    (reset! model/current-player nil)))

(defn play-radio [radio]
  (stop-radio)
  (try
    (let [url (URL. (:url radio))
          input-stream (.openStream url)
          player (AdvancedPlayer. input-stream)]
      (reset! model/current-player player)
      (.start (Thread. #(.play player))))
    (catch Exception e
      (println "Erreur de lecture:" (.getMessage e)))))

(comment
  (play-radio {:url "http://direct.franceinter.fr/live/franceinter-midfi.mp3"})
  (stop-radio))