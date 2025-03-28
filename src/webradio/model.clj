(ns webradio.model
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def radios-file "radios.edn")

(defn default-radios []
  [{:name "Radio FIP" :url "http://direct.fipradio.fr/live/fip-midfi.mp3"}
   {:name "France Inter" :url "http://direct.franceinter.fr/live/franceinter-midfi.mp3"}
   {:name "Nova" :url "http://radionova.ice.infomaniak.ch/radionova-256.aac"}
   {:name "Shonan Beach FM" :url "http://shonanbeachfm.out.airtime.pro:8000/shonanbeachfm_a"}
   {:name "CHOI 981 Radio X" :url "https://lb0-stream.radiox981.ca/choi.mp3"}])

(defn load-radios []
  (try
    (if (.exists (io/file radios-file))
      (with-open [r (java.io.PushbackReader. (io/reader radios-file))]
        (or (edn/read r) (default-radios)))
      (default-radios))
    (catch Exception e
      (println "Error loading radios:" (.getMessage e))
      (default-radios))))

(def radios (atom (load-radios)))

(def current-player (atom nil))

(defn save-radios! []
  (try
    (io/make-parents radios-file)
    (spit radios-file (pr-str @radios))
    true
    (catch Exception e
      (println "Error saving radios:" (.getMessage e))
      false)))

(defn add-radio! [name url]
  (when (and (not-empty name) (not-empty url))
    (let [new-radio {:name name :url url}]
      (swap! radios conj new-radio)
      (save-radios!)
      true)))
