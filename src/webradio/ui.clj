(ns webradio.ui
  (:require [webradio.model :as model]
            [webradio.player :as player])
  (:import [javax.swing JFrame JList JScrollPane JButton]))

(defn create-ui []
  (let [frame (JFrame. "WebRadio Player")
        radio-list (JList. (into-array String (map :name @model/radios)))
        scroll-pane (JScrollPane. radio-list)
        play-button (JButton. "Play")
        stop-button (JButton. "Stop")]

    ;; Configuration des écouteurs
    (.addListSelectionListener radio-list
                               (reify javax.swing.event.ListSelectionListener
                                 (valueChanged [_ event]
                                   (when-not (.getValueIsAdjusting event)
                                     (let [selected-index (.getSelectedIndex radio-list)
                                           selected-radio (get @model/radios selected-index)]
                                       (player/play-radio selected-radio))))))

    (.addActionListener play-button
                        (reify java.awt.event.ActionListener
                          (actionPerformed [_ _]
                            (when-let [selected-radio (first (filter #(= (.getSelectedValue radio-list) (:name %)) @model/radios))]
                              (player/play-radio selected-radio)))))

    (.addActionListener stop-button
                        (reify java.awt.event.ActionListener
                          (actionPerformed [_ _]
                            (player/stop-radio))))

    ;; Organisation du layout
    (doto frame
      (.setLayout (java.awt.BorderLayout.))
      (.add scroll-pane java.awt.BorderLayout/CENTER)
      (.add play-button java.awt.BorderLayout/WEST)
      (.add stop-button java.awt.BorderLayout/EAST)
      (.setSize 400 300)
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.setVisible true))))