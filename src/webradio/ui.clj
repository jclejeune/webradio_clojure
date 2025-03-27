(ns webradio.ui
  (:require [webradio.model :as model]
            [webradio.player :as player]
            [clojure.java.io :as io])
  (:import [javax.swing JFrame JList JScrollPane JButton JPanel JLabel]
           [java.awt Font]
           [java.awt Color BorderLayout FlowLayout]
           [javax.swing.border LineBorder]))

(defn get-selected-radio-index [radio-list]
  (.getSelectedIndex radio-list))

(defn load-digital-font [size]
  (try
    (let [font-file (io/resource "fonts/VCR.ttf")]
      (if font-file
        (let [digital-font (Font/createFont Font/TRUETYPE_FONT (io/input-stream font-file))]
          (.deriveFont digital-font (float size)))
        (Font. "Monospaced" Font/BOLD size)))
    (catch Exception _
      (Font. "Monospaced" Font/BOLD size))))

(defn create-ui []
  (let [frame (JFrame. "WebRadio Player")
        radio-list (JList. (into-array String (map :name @model/radios)))
        scroll-pane (JScrollPane. radio-list)

        play-button (JButton. "▶")
        prev-button (JButton. "⏮")  ; Previous button
        next-button (JButton. "⏭")  ; Next button
        stop-button (JButton. "⏹")  ; Stop button

        control-panel (JPanel.)
        digital-font (load-digital-font 24)
        status-label (JLabel. "--")]

    ;; Autoradio-style status label
    (doto status-label
      (.setOpaque true)
      (.setBackground (Color. 0 0 0))  ; Very dark background
      (.setForeground (Color. 250 139 1))   ; Green text
      (.setFont digital-font)
      (.setBorder (LineBorder. (Color. 80 80 80) 2))
      (.setHorizontalAlignment JLabel/CENTER)
      (.setPreferredSize (java.awt.Dimension. 300 100)))

    ;; List selection listener
    (.addListSelectionListener radio-list
                               (reify javax.swing.event.ListSelectionListener
                                 (valueChanged [_ event]
                                   (when-not (.getValueIsAdjusting event)
                                     (let [selected-index (get-selected-radio-index radio-list)
                                           selected-radio (get @model/radios selected-index)]

              ;; Enable/disable prev/next buttons based on selection
                                       (doto prev-button
                                         (.setEnabled (not= selected-index 0)))
                                       (doto next-button
                                         (.setEnabled (not= selected-index (dec (count @model/radios)))))

                                       (player/play-radio selected-radio)
                                       (.setText status-label (:name selected-radio)))))))

    ;; Previous button listener
    (.addActionListener prev-button
                        (reify java.awt.event.ActionListener
                          (actionPerformed [_ _]
                            (let [current-index (get-selected-radio-index radio-list)]
                              (when (pos? current-index)
                                (.setSelectedIndex radio-list (dec current-index)))))))

    ;; Next button listener
    (.addActionListener next-button
                        (reify java.awt.event.ActionListener
                          (actionPerformed [_ _]
                            (let [current-index (get-selected-radio-index radio-list)]
                              (when (< current-index (dec (count @model/radios)))
                                (.setSelectedIndex radio-list (inc current-index)))))))

    ;; Play button listener
    (.addActionListener play-button
                        (reify java.awt.event.ActionListener
                          (actionPerformed [_ _]
                            (when-let [selected-radio (first (filter #(= (.getSelectedValue radio-list) (:name %)) @model/radios))]
                              (player/play-radio selected-radio)
                              (.setText status-label  (:name selected-radio))))))

    ;; Stop button listener
    (.addActionListener stop-button
                        (reify java.awt.event.ActionListener
                          (actionPerformed [_ _]
                            (player/stop-radio)
                            (.setText status-label "--"))))

    ;; Initial button states
    (doto prev-button (.setEnabled false))
    (doto next-button
      (.setEnabled (> (count @model/radios) 1)))

    (doto control-panel
      (.setLayout (FlowLayout.))
      (.setBackground (Color. 240 240 240))
      (.add prev-button)
      (.add play-button)
      (.add next-button)
      (.add stop-button))

    (doto frame
      (.setLayout (BorderLayout.))
      (.add scroll-pane BorderLayout/CENTER)
      (.add control-panel BorderLayout/SOUTH)
      (.add status-label BorderLayout/NORTH)
      (.setSize 400 400)
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.setVisible true))))