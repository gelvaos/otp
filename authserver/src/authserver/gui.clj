(ns authserver.gui
  (:import
   [javax.swing JLabel JButton JPanel JFrame])
  (:use [seesaw core mig]
        [authserver.password])
  (:gen-class :main true))

(native!)

(defn set-secret-key [key]
  (get-next-password key))

(def passwd-text-field (text ""))
(def secret-key-text-field (text :columns 30))
(def current-passwd-text-field (text :columns 30))

(defn form-content []
  (mig-panel :constraints ["wrap 3", "[right]"]
             :items [
                     [ "Input: "                  "split, span, gaptop 10"]
                     [ :separator                 "growx, wrap, gaptop 10"]

                     [ "Password:"                "gap 10"]
                     [ passwd-text-field          "growx"]
                     [ (button :id :login
                               :text "Login")     "gap 10, wrap"]

                     [ "Internal state (for demo purpose only): "
                       "split, span, gaptop 10"]
                     [ :separator                 "growx, wrap, gaptop 10"]

                     [ "Secret key:"              "gap 10"]
                     [ secret-key-text-field      "growx"]
                     [ (button :id :set-key
                               :text "Set Key")   "gap 10, wrap"]
                     [ "Current password:"        "gap 10"]
                     [ current-passwd-text-field  "growx, wrap"]
                     [ :separator                 "growx, wrap, gaptop 20"]
                     [ (button :id :close
                               :text "Close")     "gaptop 10, wrap"]]))


(defn add-behaviours[root]
  (listen (select root [:#login])
          :action (fn [e]
                    (let [current-passwd
                          (text current-passwd-text-field)]
                      (if (not= (text passwd-text-field)
                                current-passwd)
                        (alert "Wrong Password!")
                        (do
                          (alert "Login sucessful!")
                          (text! secret-key-text-field current-passwd)
                          (text! current-passwd-text-field
                                 (set-secret-key current-passwd)))))))
  (listen (select root [:#set-key])
          :action (fn [e]
                    (let [key
                          (set-secret-key
                           (text secret-key-text-field))]
                      (text! current-passwd-text-field key))))
  (listen (select root [:#close]) :action (fn [e] (dispose! root)))
  root)

(defn -main [& args]
  (invoke-later
    (->
     (frame :title "One-Time-Password authentication server prototype"
            :content (form-content)
            :on-close :exit)
     add-behaviours
     pack!
     show!)))




