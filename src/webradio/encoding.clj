(ns webradio.encoding
  (:import [org.mozilla.universalchardet UniversalDetector]))

(defn detect-encoding [bytes]
  (let [detector (UniversalDetector. nil)]
    (.handleData detector bytes 0 (count bytes))
    (.dataEnd detector)
    (let [encoding (.getDetectedCharset detector)]
      (or encoding "UTF-8"))))  ;; Par défaut, UTF-8

(defn fix-encoding [bytes]
  (let [encoding (detect-encoding bytes)]
    (String. bytes encoding)))
