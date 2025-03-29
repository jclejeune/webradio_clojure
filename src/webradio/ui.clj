(ns webradio.ui
  (:require [webradio.model :as model]
            [webradio.player :as player]
            [webradio.theme :as theme])
  (:import [javax.swing JFrame JList JScrollPane JPanel JTextField JLabel JOptionPane DefaultListModel ImageIcon]
           [javax.swing.event ListSelectionListener]
           [java.awt BorderLayout FlowLayout GridLayout]
           [java.awt.event ActionListener]))

(defn- update-radio-list! [jlist radios]
  (let [model (DefaultListModel.)]
    (doseq [radio radios]
      (.addElement model (:name radio)))
    (.setModel jlist model)))

(defn resize-icon [icon height]
  (let [image (.getImage icon)
        width  (int (* (/ height (float (.getHeight image))) (.getWidth image)))]
    (ImageIcon. (.getScaledInstance image width height java.awt.Image/SCALE_SMOOTH))))

;; Redimensionnement des icônes
(def play-button-icon (resize-icon (ImageIcon. "resources/images/play-icon.png") 25))
(def stop-button-icon (resize-icon (ImageIcon. "resources/images/stop-icon.png") 25))
(def forward-button-icon (resize-icon (ImageIcon. "resources/images/forward-icon.png") 25))
(def backward-button-icon (resize-icon (ImageIcon. "resources/images/backward-icon.png") 25))

(defn create-ui []
  (theme/apply-dark-theme)

  (let [frame (JFrame. "WebRadio Player")
        add-panel (JPanel. (GridLayout. 2 2 5 5))
        name-field (doto (JTextField. 20)
                     (.setFont (java.awt.Font. "Arial" java.awt.Font/ITALIC 12))
                     (.setForeground (:foreground theme/dark-theme-colors))  ; Fix: use the Color object here
                     (.setText "Radio")) ; Placeholder en italique
        url-field (doto (JTextField. 20)
                    (.setFont (java.awt.Font. "Arial" java.awt.Font/ITALIC 12))
                    (.setForeground (:foreground theme/dark-theme-colors))  ; Fix: use the Color object here
                    (.setText "URL")) ; Placeholder en italique
        add-button (theme/styled-button "+Add" theme/dark-theme-colors)
        radio-list (JList.)
        scroll-pane (JScrollPane. radio-list)
        digital-font (theme/load-digital-font 24)
        status-label (theme/styled-label "--" digital-font theme/dark-theme-colors)

        ;; Création des JLabel pour les icônes
        play-label (JLabel. play-button-icon)
        stop-label (JLabel. stop-button-icon)
        next-label (JLabel. forward-button-icon)
        prev-label (JLabel. backward-button-icon)

        control-panel (JPanel.)
        form-panel (JPanel. (BorderLayout.))
        main-panel (JPanel. (BorderLayout.))]

    ;; Configuration du formulaire
    (doto add-panel
      (.setBackground (:background theme/dark-theme-colors))
      ;; (.add (JLabel. "Radio:"))
      (.add name-field)
      ;; (.add (JLabel. "URL:"))
      (.add url-field))

    (doto form-panel
      (.add add-button BorderLayout/EAST)
      (.add add-panel BorderLayout/CENTER))

    ;; Configuration des panneaux de contrôle (remplacer les boutons par des icônes)
    (doto control-panel
      (.setLayout (FlowLayout.))
      (.setBackground (:background theme/dark-theme-colors))
      (.add prev-label)
      (.add play-label)
      (.add next-label)
      (.add stop-label))

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

    ;; Play Label Action
    (.addMouseListener play-label
                       (proxy [java.awt.event.MouseAdapter] []
                         (mouseClicked [e]
                           (let [index (.getSelectedIndex radio-list)]
                             (when (>= index 0)
                               (player/play-radio (nth @model/radios index))
                               (.setText status-label (.getSelectedValue radio-list)))))))

    ;; Stop Label Action
    (.addMouseListener stop-label
                       (proxy [java.awt.event.MouseAdapter] []
                         (mouseClicked [e]
                           (player/stop-radio)
                           (.setText status-label "--"))))

    ;; Previous Label Action
    (.addMouseListener prev-label
                       (proxy [java.awt.event.MouseAdapter] []
                         (mouseClicked [e]
                           (let [current (.getSelectedIndex radio-list)]
                             (when (> current 0)
                               (.setSelectedIndex radio-list (dec current)))))))

    ;; Next Label Action
    (.addMouseListener next-label
                       (proxy [java.awt.event.MouseAdapter] []
                         (mouseClicked [e]
                           (let [current (.getSelectedIndex radio-list)
                                 max-index (dec (count @model/radios))]
                             (when (< current max-index)
                               (.setSelectedIndex radio-list (inc current)))))))

    ;; Add Button Action
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
