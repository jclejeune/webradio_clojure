(ns webradio.player
  (:require [webradio.model :as model])
  (:import [javazoom.jl.player Player]
           [java.net URL]))

(defn stop-radio []
  (when @model/current-player
    (.close @model/current-player)
    (reset! model/current-player nil)))

(defn play-radio [radio]
  (stop-radio)
  (let [url (URL. (:url radio))
        input-stream (.openStream url)
        player (Player. input-stream)]
    (reset! model/current-player player)
    (.start (Thread. #(.play player)))))