(ns webradio.model)

(def radios
  (atom
   [{:name "Radio FIP" :url "http://direct.fipradio.fr/live/fip-midfi.mp3"}
    {:name "France Inter" :url "http://direct.franceinter.fr/live/franceinter-midfi.mp3"}
    {:name "Nova" :url "http://radionova.ice.infomaniak.ch/radionova-256.aac"}
    {:name "Shonan Beach FM" :url "http://shonanbeachfm.out.airtime.pro:8000/shonanbeachfm_a"}
    {:name "CHOI 981 Radio X" :url "https://lb0-stream.radiox981.ca/choi.mp3"}
    ]))

(def current-player (atom nil))