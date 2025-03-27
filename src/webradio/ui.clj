(ns webradio.ui
  (:require [webradio.model :as model]
            [webradio.player :as player]
            [webradio.theme :as theme])
  (:import [javax.swing JFrame JList JScrollPane JPanel]
           [javax.swing.event ListSelectionListener]
           [java.awt BorderLayout FlowLayout]
           [java.awt.event ActionListener]))

(defn create-ui []
  (theme/apply-dark-theme)

  (let [frame (JFrame. "WebRadio Player")
        radio-list (JList. (into-array String (map :name @model/radios)))
        scroll-pane (JScrollPane. radio-list)
        digital-font (theme/load-digital-font 24)
        status-label (theme/styled-label "--" digital-font theme/dark-theme-colors)
        play-button (theme/styled-button "▶" theme/dark-theme-colors)
        prev-button (theme/styled-button "⏮" theme/dark-theme-colors)
        next-button (theme/styled-button "⏭" theme/dark-theme-colors)
        stop-button (theme/styled-button "⏹" theme/dark-theme-colors)
        control-panel (JPanel.)]

    ;; Gestion des événements
    (.addListSelectionListener radio-list
                               (reify ListSelectionListener
                                 (valueChanged [_ e]
                                   (when-not (.getValueIsAdjusting e)
                                     (let [index (.getSelectedIndex radio-list)]
                                       (when (>= index 0)
                                         (player/play-radio (nth @model/radios index))
                                         (.setText status-label (.getSelectedValue radio-list))))))))

    (.addActionListener play-button
                        (reify ActionListener
                          (actionPerformed [_ _]
                            (when-let [index (.getSelectedIndex radio-list)]
                              (player/play-radio (nth @model/radios index))))))

    (.addActionListener stop-button
                        (reify ActionListener
                          (actionPerformed [_ _]
                            (player/stop-radio)
                            (.setText status-label "--"))))

    (.addActionListener prev-button
                        (reify ActionListener
                          (actionPerformed [_ _]
                            (let [current (.getSelectedIndex radio-list)]
                              (when (> current 0)
                                (.setSelectedIndex radio-list (dec current)))))))

    (.addActionListener next-button
                        (reify ActionListener
                          (actionPerformed [_ _]
                            (let [current (.getSelectedIndex radio-list)
                                  max-index (dec (count @model/radios))]
                              (when (< current max-index)
                                (.setSelectedIndex radio-list (inc current)))))))

    (doto control-panel
      (.setLayout (FlowLayout.))
      (.setBackground (:background theme/dark-theme-colors))
      (.add prev-button)
      (.add play-button)
      (.add next-button)
      (.add stop-button))

    (doto frame
      (.setLayout (BorderLayout.))
      (.add scroll-pane BorderLayout/CENTER)
      (.add control-panel BorderLayout/SOUTH)
      (.add status-label BorderLayout/NORTH)
      (.setSize 400 500)
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.setVisible true))))
