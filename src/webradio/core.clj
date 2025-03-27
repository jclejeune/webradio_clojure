(ns webradio.core
  (:require [webradio.ui :as ui])
  (:gen-class))

(defn -main [& _]
  (ui/create-ui))