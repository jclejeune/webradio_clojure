(ns webradio.ui
  (:require [webradio.model :as model]
            [webradio.player :as player]
            [webradio.theme :as theme])
  (:import [javax.swing JFrame JList JScrollPane JPanel JTextField JLabel JOptionPane DefaultListModel]
           [javax.swing.event ListSelectionListener]
           [java.awt BorderLayout FlowLayout GridLayout]
           [java.awt.event ActionListener]))

(defn- update-radio-list! [jlist radios]
  (let [model (DefaultListModel.)]
    (doseq [radio radios]
      (.addElement model (:name radio)))
    (.setModel jlist model)))

(defn create-ui []
  (theme/apply-dark-theme)

  (let [frame (JFrame. "WebRadio Player")
        add-panel (JPanel. (GridLayout. 2 2 5 5))
        name-field (JTextField.)
        url-field (JTextField.)
        add-button (theme/styled-button "+ Ajouter" theme/dark-theme-colors)
        radio-list (JList.)
        scroll-pane (JScrollPane. radio-list)
        digital-font (theme/load-digital-font 24)
        status-label (theme/styled-label "--" digital-font theme/dark-theme-colors)

        ; Restore to previous button creation method
        play-button (theme/styled-button "▶" theme/dark-theme-colors)
        prev-button (theme/styled-button "⏮" theme/dark-theme-colors)
        next-button (theme/styled-button "⏭" theme/dark-theme-colors)
        stop-button (theme/styled-button "⏹" theme/dark-theme-colors)

        control-panel (JPanel.)
        form-panel (JPanel. (BorderLayout.))
        main-panel (JPanel. (BorderLayout.))]

    ;; Configuration du formulaire
    (doto add-panel
      (.setBackground (:background theme/dark-theme-colors))
      (.add (JLabel. "Nom:"))
      (.add name-field)
      (.add (JLabel. "URL:"))
      (.add url-field))

    ;; Configuration des boutons
    (doto form-panel
      (.setBackground (:background theme/dark-theme-colors))
      (.add add-panel BorderLayout/CENTER)
      (.add add-button BorderLayout/EAST))

    ;; Initialisation de la liste
    (update-radio-list! radio-list @model/radios)

    ;; List Selection Listener
    (.addListSelectionListener
     radio-list
     (reify ListSelectionListener
       (valueChanged [_ e]
         (when-not (.getValueIsAdjusting e)
           (let [index (.getSelectedIndex radio-list)]
             (when (>= index 0)
               (player/play-radio (nth @model/radios index))
               (.setText status-label (.getSelectedValue radio-list))))))))

    ;; Play Button
    (.addActionListener play-button
                        (reify ActionListener
                          (actionPerformed [_ _]
                            (let [index (.getSelectedIndex radio-list)]
                              (when (>= index 0)
                                (player/play-radio (nth @model/radios index))
                                (.setText status-label (.getSelectedValue radio-list)))))))

    ;; Stop Button
    (.addActionListener stop-button
                        (reify ActionListener
                          (actionPerformed [_ _]
                            (player/stop-radio)
                            (.setText status-label "--"))))

    ;; Previous Button
    (.addActionListener prev-button
                        (reify ActionListener
                          (actionPerformed [_ _]
                            (let [current (.getSelectedIndex radio-list)]
                              (when (> current 0)
                                (.setSelectedIndex radio-list (dec current)))))))

    ;; Next Button
    (.addActionListener next-button
                        (reify ActionListener
                          (actionPerformed [_ _]
                            (let [current (.getSelectedIndex radio-list)
                                  max-index (dec (count @model/radios))]
                              (when (< current max-index)
                                (.setSelectedIndex radio-list (inc current)))))))

    ;; Add Button
    (.addActionListener add-button
                        (reify ActionListener
                          (actionPerformed [_ _]
                            (let [name (.getText name-field)
                                  url (.getText url-field)]
                              (if (model/add-radio! name url)
                                (do
                                  (update-radio-list! radio-list @model/radios)
                                  (.setText name-field "")
                                  (.setText url-field ""))
                                (JOptionPane/showMessageDialog frame
                                                               "Veuillez saisir un nom et une URL valides"
                                                               "Erreur"
                                                               JOptionPane/ERROR_MESSAGE))))))

    ;; Configuration des panneaux
    (doto control-panel
      (.setLayout (FlowLayout.))
      (.setBackground (:background theme/dark-theme-colors))
      (.add prev-button)
      (.add play-button)
      (.add next-button)
      (.add stop-button))

    ;; Assemblage final
    (doto main-panel
      (.add form-panel BorderLayout/NORTH)
      (.add scroll-pane BorderLayout/CENTER)
      (.add control-panel BorderLayout/SOUTH))

    (doto frame
      (.setLayout (BorderLayout.))
      (.add main-panel BorderLayout/CENTER)
      (.add status-label BorderLayout/NORTH)
      (.setSize 400 600)
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.setVisible true))))