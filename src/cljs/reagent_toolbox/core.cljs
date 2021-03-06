;;;; Copyright © 2017 Flexpoint Tech Ltd.

(ns reagent-toolbox.core
  (:refer-clojure :exclude [list])
  (:require cljsjs.react-toolbox
            [reagent.core :as reagent]
            [cljs.reader :as reader]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as csk-extras]))

(defn- as-element-by-key
  "Given a map of properties and a list of keys, if those are present and are not strings, it'll treat them as Reagent
   components and wrap them with as-element so they can be used as React components."
  [properties keys-to-componetize]
  (into {}
        (map (fn [[key value]]
               [key (if (and (some #{key} keys-to-componetize)
                             (not (string? value)))
                      (reagent/as-element value)
                      value)])
             properties)))

(def app-bar-component (reagent/adapt-react-class (.-AppBar js/ReactToolbox)))

(defn app-bar [properties children]
  (let [properties (as-element-by-key properties [:left-icon :right-icon])]
    [app-bar-component properties children]))

(def autocomplete-component (reagent/adapt-react-class (.-Autocomplete js/ReactToolbox)))

(defn autocomplete [properties]
  (let [properties (as-element-by-key properties [:error :label])]
    [autocomplete-component properties]))

(def avatar-component (reagent/adapt-react-class (.-Avatar js/ReactToolbox)))

(defn avatar [properties-or-children & [children]]
  (let [properties (if (map? properties-or-children) properties-or-children {})
        children (if (map? properties-or-children) children properties-or-children)
        properties (as-element-by-key properties [:icon :image :children])]
    [avatar-component properties children]))

(def button-component (reagent/adapt-react-class (.-Button js/ReactToolbox)))

(defn button [properties-or-children & [children]]
  (let [properties (if (map? properties-or-children) properties-or-children {})
        children (if (map? properties-or-children) children properties-or-children)
        properties (as-element-by-key properties [:icon])]
    [button-component properties children]))

(def card (reagent/adapt-react-class (.-Card js/ReactToolbox)))

(def card-actions (reagent/adapt-react-class (.-CardActions js/ReactToolbox)))

(def card-media (reagent/adapt-react-class (.-CardMedia js/ReactToolbox)))

(def card-text (reagent/adapt-react-class (.-CardText js/ReactToolbox)))

(def card-title (reagent/adapt-react-class (.-CardTitle js/ReactToolbox)))

(def checkbox-component (reagent/adapt-react-class (.-Checkbox js/ReactToolbox)))

(defn checkbox [properties]
  (let [properties (as-element-by-key properties [:checked :label])]
    [checkbox-component properties]))

(def chip (reagent/adapt-react-class (.-Chip js/ReactToolbox)))

(def date-picker (reagent/adapt-react-class (.-DatePicker js/ReactToolbox)))

(def dialog-component (reagent/adapt-react-class (.-Dialog js/ReactToolbox)))

(defn dialog [properties & children]
  (let [properties (if-let [actions (:actions properties)]
                     (assoc properties :actions (csk-extras/transform-keys csk/->camelCaseString actions))
                     properties)]
    (into [dialog-component properties] children)))

(def drawer-component (reagent/adapt-react-class (.-Drawer js/ReactToolbox)))

(defn drawer [properties & children]
  (into [drawer-component properties] children))

(def dropdown-component (reagent/adapt-react-class (.-Dropdown js/ReactToolbox)))

(defn dropdown [properties]
  (let [serialize-value pr-str
        deserialize-value reader/read-string
        source (map #(assoc % :value (serialize-value (:value %)))
                    (:source properties))
        properties (assoc properties :source source)
        properties (if (:on-change properties)
                     (assoc properties :on-change (fn [value]
                                                    ((:on-change properties) (deserialize-value value))))
                     properties)
        properties (if (:value properties)
                     (assoc properties :value (serialize-value (:value properties)))
                     properties)]
    [dropdown-component properties]))

(def font-icon-component (reagent/adapt-react-class (.-FontIcon js/ReactToolbox)))

(defn font-icon [properties]
  (let [properties (if (map? properties)
                     (as-element-by-key properties [:value])
                     properties)]
    [font-icon-component properties]))

(def icon-button-component (reagent/adapt-react-class (.-IconButton js/ReactToolbox)))

(defn icon-button [properties-or-children & [children]]
  (let [properties (if (map? properties-or-children) properties-or-children {})
        children (if (map? properties-or-children) children properties-or-children)
        properties (as-element-by-key properties [:icon])]
    [icon-button-component properties children]))

(def icon-menu (reagent/adapt-react-class (.-IconMenu js/ReactToolbox)))

(def input-component (reagent/adapt-react-class (.-Input js/ReactToolbox)))

(defn input [{:keys [value on-change] :as _props}]
  ; Context for what's going on here:
  ; https://github.com/reagent-project/reagent/blob/b65afde4d7ac4864d7e355acdc16977cb92afa3c/src/reagent/impl/template.cljs#L99
  ; https://gist.github.com/metametadata/3b4e9d5d767dfdfe85ad7f3773696a60#file-react-bootstrap-cljs-L24-L50
  ; https://stackoverflow.com/questions/28922275/in-reactjs-why-does-setstate-behave-differently-when-called-synchronously/28922465#28922465
  (let [local-value (atom value)]                           ; regular atom is used instead of React's state to better control when renders should be triggered
    (reagent/create-class
      {:display-name            "ReagentToolboxInput"
       :should-component-update (fn [_ [_ old-props] [_ new-props]]

                                  (if (not= (:value new-props) @local-value) ; Update only if value is different from the rendered one or...
                                    (do
                                      (reset! local-value (:value new-props))
                                      true)
                                    (not= (dissoc new-props :value) ; other props changed
                                          (dissoc old-props :value))))

       :reagent-render          (fn [argv _comp _jsprops _first-child]
                                  (let [component (reagent/current-component)
                                        input-hacked-properties (when (and (contains? argv :value)
                                                                           (contains? argv :on-change))
                                                                  (assoc argv
                                                                    :value @local-value ; use value only from the local atom
                                                                    :on-change (fn wrapped-on-change [value event] ; render immediately to sync DOM and virtual DOM
                                                                                 (reset! local-value value)
                                                                                 (reagent/force-update component)
                                                                                 ((:on-change argv) value event)))) ; this will presumably update the value in global state atom
                                        properties (or input-hacked-properties argv)
                                        properties (as-element-by-key properties [:error :hint :icon :label])]
                                    [input-component properties]))})))

(def layout (reagent/adapt-react-class (.-Layout js/ReactToolbox)))

(def link-component (reagent/adapt-react-class (.-Link js/ReactToolbox)))

(defn link [properties]
  (let [properties (as-element-by-key properties [:active :count :href :icon :label])]
    [link-component properties]))

(def list (reagent/adapt-react-class (.-List js/ReactToolbox)))

(def list-checkbox (reagent/adapt-react-class (.-ListCheckbox js/ReactToolbox)))

(def list-divider (reagent/adapt-react-class (.-ListDivider js/ReactToolbox)))

(def list-item-component (reagent/adapt-react-class (.-ListItem js/ReactToolbox)))

(defn list-item [properties]
  (let [args (assoc properties
               :left-actions (map reagent/as-element (:left-actions properties))
               :right-actions (map reagent/as-element (:right-actions properties)))]
    [list-item-component args]))

(def list-sub-header (reagent/adapt-react-class (.-ListSubHeader js/ReactToolbox)))

(def menu-divider (reagent/adapt-react-class (.-MenuDivider js/ReactToolbox)))

(def menu-item (reagent/adapt-react-class (.-MenuItem js/ReactToolbox)))

(def nav-drawer (reagent/adapt-react-class (.-NavDrawer js/ReactToolbox)))

(def navigation-component (reagent/adapt-react-class (.-Navigation js/ReactToolbox)))

(defn navigation [properties-or-children & children]
  (let [properties (if (map? properties-or-children) properties-or-children {})
        children (if (map? properties-or-children) children properties-or-children)
        properties (as-element-by-key properties [:type])]
    (into [navigation-component properties] children)))

(def panel (reagent/adapt-react-class (.-Panel js/ReactToolbox)))

(def progress-bar-component (reagent/adapt-react-class (.-ProgressBar js/ReactToolbox)))

(defn progress-bar [properties]
  (let [properties (as-element-by-key properties [:type :mode :value :buffer])]
    [progress-bar-component properties]))

(def radio-button-component (reagent/adapt-react-class (.-RadioButton js/ReactToolbox)))

(defn radio-button [properties]
  (let [properties (as-element-by-key properties [:label :value :disabled :checked])]
    [radio-button-component properties]))

(def radio-group-component (reagent/adapt-react-class (.-RadioGroup js/ReactToolbox)))

(defn radio-group [properties-or-children & children]
  ; Sadly, radio-groups don't just work. It looks like the Radio Buttons and the Radio Groups cannot find each other.
  ; That means we have te re-implement the automatic selection mechanism here.
  ; Likely related to: https://github.com/react-toolbox/react-toolbox/issues/1361
  (let [properties (if (map? properties-or-children) properties-or-children {})
        children (if (map? properties-or-children) children properties-or-children)
        children (if-let [on-change (:on-change properties)]
                   (map (fn [child]
                          (-> child
                              (assoc-in [1 :on-change] (fn [] (on-change (:value (second child)))))
                              (assoc-in [1 :checked] (= (:value properties) (:value (second child))))))
                        children))]
    (into [radio-group-component properties-or-children] children)))

(def ripple (reagent/adapt-react-class (.-Ripple js/ReactToolbox)))

(def sidebar (reagent/adapt-react-class (.-Sidebar js/ReactToolbox)))

(def slider-component (reagent/adapt-react-class (.-Slider js/ReactToolbox)))

(defn slider [properties]
  (let [properties (as-element-by-key properties [:value])]
    [slider-component properties]))

(def snackbar-component (reagent/adapt-react-class (.-Snackbar js/ReactToolbox)))

(defn snackbar [properties]
  [snackbar-component properties])

(def switch-component (reagent/adapt-react-class (.-Switch js/ReactToolbox)))

(defn switch [properties]
  (let [properties (as-element-by-key properties [])]
    [switch-component properties]))

(def tab (reagent/adapt-react-class (.-Tab js/ReactToolbox)))

(def table (reagent/adapt-react-class (.-Table js/ReactToolbox)))

(def tabs (reagent/adapt-react-class (.-Tabs js/ReactToolbox)))

(def time-picker (reagent/adapt-react-class (.-TimePicker js/ReactToolbox)))

(def tooltip (reagent/adapt-react-class (.-Tooltip js/ReactToolbox)))
