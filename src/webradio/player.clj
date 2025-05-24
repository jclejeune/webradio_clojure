(ns webradio.player
  (:require [webradio.model :as model]
            [webradio.metadata :as metadata])
  (:import [javazoom.jl.player.advanced AdvancedPlayer]
           [java.net URL]))

;; === État du player ===
(defonce playing? (atom false)) ;; Garder le nom original pour compatibilité
(defonce ^:private current-volume (atom 0.5))
(defonce ^:private playback-thread (atom nil))

;; === Gestion du volume ===
(defn set-volume!
  "Définit le volume (entre 0.0 et 1.0)"
  [volume]
  (let [normalized-volume (max 0.0 (min 1.0 volume))]
    (reset! current-volume normalized-volume)
    (println (format "Volume défini à: %.0f%%" (* normalized-volume 100)))))

(defn get-volume
  "Retourne le volume actuel"
  []
  @current-volume)

;; === Gestion de la lecture ===
(defn- cleanup-player!
  "Nettoie les ressources du player"
  []
  (when-let [player @model/current-player]
    (try
      (.close player)
      (catch Exception e
        (println "Erreur lors de la fermeture du player:" (.getMessage e))))
    (reset! model/current-player nil))
  (reset! playing? false)
  (reset! playback-thread nil))

(defn- create-playback-thread
  "Crée le thread de lecture audio"
  [player]
  (Thread.
   (fn []
     (try
       (println "Démarrage de la lecture...")
       (.play player)
       (println "Lecture terminée")
       (catch Exception e
         (println "Erreur durant la lecture:" (.getMessage e)))
       (finally
         (cleanup-player!)
         (metadata/stop-metadata-fetcher))))))

(defn stop-radio
  "Arrête la lecture de la radio"
  []
  (when @playing?
    (println "Arrêt de la radio...")
    (reset! playing? false)

    ;; Arrêter les métadonnées
    (metadata/stop-metadata-fetcher)

    ;; Interrompre le thread de lecture s'il existe
    (when-let [thread @playback-thread]
      (when (.isAlive thread)
        (.interrupt thread)))

    ;; Nettoyer le player
    (cleanup-player!)))

(defn play-radio
  "Lance la lecture d'une radio"
  [radio]
  (when (nil? radio)
    (throw (IllegalArgumentException. "Radio ne peut pas être nil")))

  (println (format "Lecture de: %s (%s)" (:name radio) (:url radio)))

  ;; Arrêter la lecture actuelle proprement
  (stop-radio)

  (try
    (let [url (URL. (:url radio))
          input-stream (.openStream url)
          player (AdvancedPlayer. input-stream)]

      ;; Mettre à jour l'état
      (reset! playing? true)
      (reset! model/current-player player)

      ;; Démarrer la récupération des métadonnées
      (metadata/start-metadata-fetcher (:url radio))

      ;; Créer et démarrer le thread de lecture
      (let [thread (create-playback-thread player)]
        (reset! playback-thread thread)
        (.start thread)))

    (catch Exception e
      (println "Erreur lors du démarrage de la lecture:" (.getMessage e))
      (cleanup-player!)
      (throw e))))

;; === Utilitaires ===
(defn is-playing?
  "Retourne true si une radio est en cours de lecture"
  []
  @playing?)

(defn get-current-radio
  "Retourne la radio actuellement en cours de lecture (ou nil)"
  []
  (when @playing?
    ;; Note: il faudrait stocker la radio courante pour pouvoir la retourner
    ;; Pour l'instant on ne peut que savoir si on joue ou pas
    nil))

;; === Fonctions de debug/test ===
(comment
  ;; Test de lecture
  (play-radio {:name "France Inter"
               :url "http://direct.franceinter.fr/live/franceinter-midfi.mp3"})

  ;; Attendre un peu puis arrêter
  (Thread/sleep 5000)
  (stop-radio)

  ;; Test de volume
  (set-volume! 0.8)
  (get-volume)

  ;; Test d'état
  (is-playing?))