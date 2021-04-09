(ns app.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]]))

;; ***** GLOBAL VARIABLE *****
(def SERVER_ADDRESS "http://localhost:3000")

;; ***** APPLICATION STATE *****
(defn init-appstate []
  {:available-letters ""
   :word ""
   :error ""
   :scramble nil})

(defonce appstate
  (r/atom (init-appstate)))

(defn set-field! [key field-value]
  (swap!
   appstate
   (fn [cur-appstate]
     (update cur-appstate key (fn [] field-value)))))

(defn set-error! [error-msg]
  (swap!
   appstate
   (fn [cur-appstate]
     (update cur-appstate :error (fn [] error-msg)))))

(defn set-scramble! [bool]
  (swap!
   appstate
   (fn [cur-appstate]
     (update cur-appstate :scramble (fn [] bool)))))

;; ***** NETWORK COMMUNICATION *****
(defn request []
  (go (let [available-letters (@appstate :available-letters)
            word (@appstate :word)
            response (<! (http/post SERVER_ADDRESS
                          {:with-credentials? false
                           :query-params {"available-letters" available-letters
                                          "word" word}}))
            status (:status response)]
        (case status
          400 (set-error! (:body response))
          200 (do
                (set-error! "")
                (set-scramble! (if (= (:body response) "true") true false)))
          (set-error! (str "Not handle error. Status: " status))))))

;; ***** GRAPHICAL INTERFACE *****
(defn input-text [placeholder k]
  [:input {:style {:margin-left "auto"
                   :margin-right "auto"
                   :display "block"}
           :type "text"
           :placeholder placeholder
           :value (k @appstate)
           :on-change
           (fn [evt]
             (let [field-value (-> evt .-target .-value)]
               (set-field! k field-value)))}])

(defn error-message []
  (let [error-msg (:error @appstate)]
    (when (not (empty? error-msg))
      [:div {:style {:text-align "center"
                     :color "red"}}
       (str "Error: " error-msg)])))

(defn result-message []
  (let [scramble? (:scramble @appstate)
        error-msg (:error @appstate)]
    (when (and (empty? error-msg)
               (not (nil? scramble?)))
      (if scramble?
        [:div {:style {:text-align "center" :color "green"}} "Yes, scramble!"]
        [:div {:style {:text-align "center" :color "red"}} "Not scramble :("]))))

(defn submit []
  [:input {:type "button" :value "scramble?"
           :on-click request}])

(defn title []
  [:h1 {:style {:text-align "center"}} "Scramble?"])

(defn form []
  [:div {:style {:display "flex"
                 :justify-content "space-between"
                 :max-width "700px"
                 :width "100%"
                 :margin-left "auto"
                 :margin-right "auto"}}
   [:div {:style {:width "200px"}}
    [input-text "Available Letters" :available-letters]]
   [:div {:style {:width "200px"}}
    [input-text "Word" :word]]
   [:div {:style {:width "200px"}}
    [submit]]])

(defn app []
  [:div {:style {:margin "auto"
                 :margin-top "60px"
                 :width "60%"
                 :height "200px"
                 :border "1px solid #d2d2d270"
                 :border-radius "40px"
                 :box-shadow "0px 20px 50px 2px rgb(0 0 0 / 30%)"
                 }}
   [:div {:style {:margin-bottom "30px"}}
    [title]
    [form]]
   [error-message]
   [result-message]])

;; ***** BOILER PLATE *****
(defn ^:dev/after-load start
  []
  (rdom/render [app]
            (.getElementById js/document "app")))

(defn ^:export init
  []
  (start))
