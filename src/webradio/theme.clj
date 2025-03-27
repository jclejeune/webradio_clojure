(ns webradio.theme
  (:require [clojure.java.io :as io])  ; Ajout du require manquant
  (:import [java.awt Color Font]
           [javax.swing UIManager]
           [javax.swing.border LineBorder EmptyBorder]))

;; Ajouter la fonction load-digital-font ici
(defn load-digital-font [size]
  (try
    (let [font-file (clojure.java.io/resource "fonts/VCR.ttf")]
      (if font-file
        (let [digital-font (Font/createFont Font/TRUETYPE_FONT 
                       (clojure.java.io/input-stream font-file))]
          (.deriveFont digital-font (float size)))
        (Font. "Monospaced" Font/BOLD size)))
    (catch Exception _
      (Font. "Monospaced" Font/BOLD size))))

(def dark-theme-colors
  {:background     (Color. 60 63 65)
   :foreground     (Color. 187 187 187)
   :button-bg      (Color. 77 77 77)
   :button-fg      (Color. 220 220 220)
   :selection-bg   (Color. 75 110 175)
   :selection-fg   Color/WHITE
   :status-bg      (Color. 40 40 40)
   :status-fg      (Color. 250 139 1)
   :border         (Color. 100 100 100)})

(defn apply-dark-theme []
  (let [colors dark-theme-colors]
    (UIManager/put "Panel.background" (:background colors))
    (UIManager/put "List.background" (:background colors))
    (UIManager/put "List.foreground" (:foreground colors))
    (UIManager/put "List.selectionBackground" (:selection-bg colors))
    (UIManager/put "List.selectionForeground" (:selection-fg colors))
    (UIManager/put "ScrollPane.background" (:background colors))
    (UIManager/put "Viewport.background" (:background colors))
    (UIManager/put "Button.background" (:button-bg colors))
    (UIManager/put "Button.foreground" (:button-fg colors))
    (UIManager/put "Label.foreground" (:foreground colors))))

(defn styled-label [text font colors]
  (doto (javax.swing.JLabel. text)
    (.setOpaque true)
    (.setBackground (:status-bg colors))
    (.setForeground (:status-fg colors))
    (.setFont font)
    (.setBorder (LineBorder. (:border colors) 2))
    (.setHorizontalAlignment javax.swing.JLabel/CENTER)
    (.setPreferredSize (java.awt.Dimension. 300 100))))

(defn styled-button [text colors]
  (doto (javax.swing.JButton. text)
    (.setBackground (:button-bg colors))
    (.setForeground (:button-fg colors))
    (.setBorder (EmptyBorder. 5 15 5 15))))