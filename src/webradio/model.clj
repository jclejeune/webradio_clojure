(ns webradio.model)

(def radios
  (atom
   [{:name "Radio FIP" :url "http://direct.fipradio.fr/live/fip-midfi.mp3"}
    {:name "France Inter" :url "http://direct.franceinter.fr/live/franceinter-midfi.mp3"}
    {:name "Nova" :url "http://radionova.ice.infomaniak.ch/radionova-256.aac"}]))

(def current-player (atom nil))