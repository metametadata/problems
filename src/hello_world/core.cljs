(ns hello-world.core
  (:require [reagent.core :as r]
            [cljs-react-material-ui.core :as ui]
            [cljs-react-material-ui.icons :as ic]
    ;; wtf
            cljsjs.material-ui
            [cljs-react-material-ui.reagent :as rui]))

(defn fixed-async-input
  [original-component]
  (fn fixed-component
    [{:keys [value on-change] :as _props}]
    {:pre [(ifn? on-change)]}
    (let [local-value (atom value)]                         ; regular atom is used instead of React's state to better control when renders should be triggered
      (r/create-class
        {:display-name            "fixed-async-input-content"

         :should-component-update (fn [_ [_ old-props] [_ new-props]]
                                    ; Update only if value is different from the rendered one or...
                                    (if (not= (:value new-props) @local-value)
                                      (do
                                        (reset! local-value (:value new-props))
                                        true)

                                      ; other props changed
                                      (not= (dissoc new-props :value)
                                            (dissoc old-props :value))))

         :render                  (fn [this]
                                    [original-component
                                     (-> (r/props this)
                                         ; use value only from the local atom
                                         (assoc :value @local-value)
                                         (update :on-change
                                                 (fn wrap-on-change [original-on-change]
                                                   (fn wrapped-on-change [e]
                                                     ; render immediately to sync DOM and virtual DOM
                                                     (reset! local-value (.. e -target -value))
                                                     (r/force-update this)

                                                     ; this will presumably update the value in global state atom
                                                     (original-on-change e)))))])}))))

(def text-field-original (r/adapt-react-class (.-TextField js/MaterialUI)))
(def text-field-fixed (fixed-async-input text-field-original))

(defonce text-state (r/atom "foobar"))

(defn main []
  [rui/mui-theme-provider
   {:mui-theme (ui/get-mui-theme
                 {:palette {:text-color (ui/color :green600)}})}
   [:div
    [:div [:strong @text-state]]

    [:button
     {:on-click #(swap! text-state str " foo")}
     "update value property"]

    [text-field-fixed
     {:id                  "example"
      :value               @text-state
      :floating-label-text "floating"
      :hint-text           "hint"
      :on-change           (fn [e]
                             (js/console.log e)
                             (reset! text-state (.. e -target -value)))}]

    [:div
     [:strong "ORIGINAL:"]
     [text-field-original
      {:id                  "example"
       :value               @text-state
       :floating-label-text "floating"
       :hint-text           "hint"
       :on-change           (fn [e]
                              (reset! text-state (.. e -target -value)))}]]]])

(defn start []
  (r/render [main] (js/document.getElementById "app")))

(start)
