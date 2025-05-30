(ns webradio.ui
  (:require [webradio.model :as model]
            [webradio.player :as player]
            [webradio.theme :as theme])
  (:import [javax.swing JFrame JList JScrollPane JPanel JTextField JLabel JOptionPane DefaultListModel ImageIcon JPopupMenu JMenuItem JButton]
           [javax.swing.event ListSelectionListener]
           [java.awt BorderLayout FlowLayout GridLayout]
           [java.awt.event ActionListener MouseAdapter]))

(def ^:private currently-playing-radio (atom nil))

(defn- update-radio-list! [jlist radios]
  (let [model (DefaultListModel.)]
    (doseq [radio radios]
      (.addElement model (:name radio)))
    (.setModel jlist model)))

;; Dialogue pour ajouter une nouvelle radio
(defn- show-add-dialog [frame]
  (let [name-field (JTextField. 30)
        url-field (JTextField. 30)
        panel (JPanel. (GridLayout. 2 2 10 5))]

    (doto panel
      (.add (JLabel. "Nom:"))
      (.add name-field)
      (.add (JLabel. "URL:"))
      (.add url-field))

    (let [result (JOptionPane/showConfirmDialog frame panel "Ajouter une nouvelle radio" JOptionPane/OK_CANCEL_OPTION JOptionPane/PLAIN_MESSAGE)]
      (when (= result JOptionPane/OK_OPTION)
        (let [new-name (.getText name-field)
              new-url (.getText url-field)]
          (when (and (not-empty new-name) (not-empty new-url))
            (model/add-radio! new-name new-url)
            true))))))

;; Dialogue pour modifier une radio
(defn- show-edit-dialog [frame radio-name]
  (let [current-radio (first (filter #(= (:name %) radio-name) @model/radios))
        name-field (JTextField. (:name current-radio) 30)
        url-field (JTextField. (:url current-radio) 30)
        panel (JPanel. (GridLayout. 2 2 10 5))]

    (doto panel
      (.add (JLabel. "Nom:"))
      (.add name-field)
      (.add (JLabel. "URL:"))
      (.add url-field))

    (let [result (JOptionPane/showConfirmDialog frame panel "Modifier la radio" JOptionPane/OK_CANCEL_OPTION JOptionPane/PLAIN_MESSAGE)]
      (when (= result JOptionPane/OK_OPTION)
        (let [new-name (.getText name-field)
              new-url (.getText url-field)]
          (when (and (not-empty new-name) (not-empty new-url))
            (model/update-radio! radio-name new-name new-url)
            true))))))

;; Dialogue de confirmation pour supprimer
(defn- show-delete-dialog [frame radio-name]
  (let [result (JOptionPane/showConfirmDialog
                frame
                (str "Êtes-vous sûr de vouloir supprimer la radio '" radio-name "' ?")
                "Confirmer la suppression"
                JOptionPane/YES_NO_OPTION
                JOptionPane/PLAIN_MESSAGE)]
    (= result JOptionPane/YES_OPTION)))

;; Création du menu contextuel
(defn- create-popup-menu [frame radio-list]
  (let [popup (JPopupMenu.)
        edit-item (JMenuItem. "Modifier")
        delete-item (JMenuItem. "Supprimer")]

    ;; Action pour modifier
    (.addActionListener edit-item
                        (reify ActionListener
                          (actionPerformed [_ _]
                            (let [selected-name (.getSelectedValue radio-list)]
                              (when selected-name
                                (when (show-edit-dialog frame selected-name)
                                  (update-radio-list! radio-list @model/radios)))))))

    ;; Action pour supprimer
    (.addActionListener delete-item
                        (reify ActionListener
                          (actionPerformed [_ _]
                            (let [selected-name (.getSelectedValue radio-list)]
                              (when selected-name
                                (when (show-delete-dialog frame selected-name)
                                  (model/delete-radio! selected-name)
                                  (update-radio-list! radio-list @model/radios)))))))

    (doto popup
      (.add edit-item)
      (.add delete-item))))

;; Redimensionnement des icônes
(def play-button-icon (ImageIcon. "resources/images/play-icon.png"))
(def stop-button-icon (ImageIcon. "resources/images/stop-icon.png"))
(def forward-button-icon (ImageIcon. "resources/images/forward-icon.png"))
(def backward-button-icon (ImageIcon. "resources/images/backward-icon.png"))

(defn create-ui []
  (theme/apply-dark-theme)

  (let [frame (JFrame. "WebRadio Player")
        ;; Panneau de la barre de tâches simplifiée
        taskbar-panel (JPanel. (FlowLayout. FlowLayout/LEFT))
        add-button (theme/styled-button "Ajouter Radio" theme/dark-theme-colors)

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
        main-panel (JPanel. (BorderLayout.))

        ;; Création du menu contextuel
        popup-menu (create-popup-menu frame radio-list)]

    ;; Configuration de la barre de tâches
    (doto taskbar-panel
      (.setBackground (:background theme/dark-theme-colors))
      (.add add-button))

    ;; Configuration des panneaux de contrôle
    (doto control-panel
      (.setLayout (FlowLayout.))
      (.setBackground (:background theme/dark-theme-colors))
      (.add prev-label)
      (.add play-label)
      (.add next-label)
      (.add stop-label))

    ;; Initialisation de la liste
    (update-radio-list! radio-list @model/radios)

    ;; Ajout du menu contextuel à la liste
    (.addMouseListener radio-list
                       (proxy [MouseAdapter] []
                         (mousePressed [e]
                           (when (.isPopupTrigger e)
                             (let [index (.locationToIndex radio-list (.getPoint e))]
                               (.setSelectedIndex radio-list index)
                               (.show popup-menu radio-list (.getX e) (.getY e)))))
                         (mouseReleased [e]
                           (when (.isPopupTrigger e)
                             (let [index (.locationToIndex radio-list (.getPoint e))]
                               (.setSelectedIndex radio-list index)
                               (.show popup-menu radio-list (.getX e) (.getY e)))))))

    (.addMouseListener radio-list
                       (proxy [MouseAdapter] []
                         (mouseClicked [e]
                           ;; Ignorer les clics droits qui sont pour le menu contextuel
                           (when (= (.getButton e) java.awt.event.MouseEvent/BUTTON1)
                             (let [index (.locationToIndex radio-list (.getPoint e))
                                   selected-radio (when (>= index 0) (nth @model/radios index))]
                               (when (and selected-radio
                                          ;; Vérifier que le clic est sur un élément
                                          (.getCellBounds radio-list index index)
                                          (.contains (.getCellBounds radio-list index index) (.getPoint e))
                                          ;; Vérifier que ce n'est pas la radio déjà en cours de lecture
                                          (not= (:name selected-radio) (:name @currently-playing-radio)))
                                 ;; Jouer la radio seulement si elle est différente
                                 (player/play-radio selected-radio)
                                 ;; Mettre à jour la radio en cours
                                 (reset! currently-playing-radio selected-radio)
                                 (.setText status-label (.getSelectedValue radio-list))))))))

    ;; List Selection Listener
    (.addListSelectionListener radio-list
                               (reify ListSelectionListener
                                 (valueChanged [_ e]
                                   (when-not (.getValueIsAdjusting e)
                                     (let [index (.getSelectedIndex radio-list)]
                                       (when (>= index 0)
                                         (player/play-radio (nth @model/radios index))
                                         (.setText status-label (.getSelectedValue radio-list))))))))

    ;; Play Label Action
    (.addMouseListener play-label
                       (proxy [MouseAdapter] []
                         (mouseClicked [e]
                           (let [index (.getSelectedIndex radio-list)]
                             (when (>= index 0)
                               (player/play-radio (nth @model/radios index))
                               (.setText status-label (.getSelectedValue radio-list)))))))

    ;; Stop Label Action
    (.addMouseListener stop-label
                       (proxy [MouseAdapter] []
                         (mouseClicked [e]
                           (player/stop-radio)
                           (reset! currently-playing-radio nil)
                           (.setText status-label "--"))))

    ;; Previous Label Action
    (.addMouseListener prev-label
                       (proxy [MouseAdapter] []
                         (mouseClicked [e]
                           (let [current (.getSelectedIndex radio-list)]
                             (when (> current 0)
                               (.setSelectedIndex radio-list (dec current)))))))

    ;; Next Label Action
    (.addMouseListener next-label
                       (proxy [MouseAdapter] []
                         (mouseClicked [e]
                           (let [current (.getSelectedIndex radio-list)
                                 max-index (dec (count @model/radios))]
                             (when (< current max-index)
                               (.setSelectedIndex radio-list (inc current)))))))

    ;; Action du bouton "Ajouter Radio"
    (.addActionListener add-button
                        (reify ActionListener
                          (actionPerformed [_ _]
                            (when (show-add-dialog frame)
                              (update-radio-list! radio-list @model/radios)))))

    ;; Assemblage final
    (doto main-panel
      (.add taskbar-panel BorderLayout/NORTH)
      (.add scroll-pane BorderLayout/CENTER)
      (.add control-panel BorderLayout/SOUTH))

    (doto frame
      (.setLayout (BorderLayout.))
      (.add status-label BorderLayout/NORTH)
      (.add main-panel BorderLayout/CENTER)
      (.setSize 400 400)
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.setVisible true))))