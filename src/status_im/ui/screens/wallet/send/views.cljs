(ns status-im.ui.screens.wallet.send.views
  (:require-macros [status-im.utils.views :refer [defview letsubs]])
  (:require [clojure.string :as str]
            [status-im.components.react :as react]
            [re-frame.core :as re-frame]
            [status-im.components.button.view :as button]
            [status-im.components.status-bar :as status-bar]
            [status-im.components.toolbar-new.actions :as act]
            [status-im.components.toolbar-new.view :as toolbar]
            [status-im.components.camera :as camera]
            [status-im.utils.utils :as utils]
            [status-im.i18n :as i18n]
            [status-im.ui.screens.wallet.send.styles :as send.styles]
            [status-im.components.icons.vector-icons :as vector-icons]
            [reagent.core :as r]
            [status-im.utils.platform :as platform]
            [status-im.ui.screens.wallet.request.styles :as request.styles]
            [status-im.ui.screens.wallet.send.styles :as styles]
            [status-im.ui.screens.wallet.components.views :as components]
            [status-im.components.styles :as components.styles]
            [status-im.components.icons.vector-icons :as vi]
            [status-im.components.animation :as animation]
            [status-im.ui.screens.wallet.styles :as wallet.styles]
            [status-im.ui.screens.wallet.send.animations :as send.animations]))

(defn toolbar-view [signing?]
  [toolbar/toolbar2 {:style wallet.styles/toolbar}
   [toolbar/nav-button (act/back-white (if signing?
                                         #(do (re-frame/dispatch [:wallet/discard-transaction])
                                              (act/default-handler))
                                         act/default-handler))]
   [toolbar/content-title {:color :white} (i18n/label :t/send-transaction)]])

(defn sign-later []
  (utils/show-question
    (i18n/label :t/sign-later-title)
    (i18n/label :t/sign-later-text)
    #(re-frame/dispatch [:wallet/sign-transaction true])))

(defview sign-panel []
  (letsubs [account [:get-current-account]
            wrong-password? [:get-in [:wallet/send-transaction :wrong-password?]]
            signing-phrase (:signing-phrase @account)
            bottom-value (animation/create-value -250)
            opacity-value (animation/create-value 0)]
    {:component-did-mount #(send.animations/animate-sign-panel opacity-value bottom-value)}
    [react/animated-view {:style (styles/animated-sign-panel bottom-value)}
      [react/animated-view {:style (styles/sign-panel opacity-value)}
       [react/view styles/signing-phrase-container
        [react/text {:style styles/signing-phrase} signing-phrase]]
       [react/text {:style styles/signing-phrase-description} (i18n/label :t/signing-phrase-description)]
       [react/view styles/password-container
        [react/text-input
         {:auto-focus             true
          :secure-text-entry      true
          :placeholder            (i18n/label :t/enter-password)
          :placeholder-text-color "#939ba1"
          :on-change-text         #(do
                                     (re-frame/dispatch [:set-in [:wallet/send-transaction :password] %]))
          :style                  styles/password}]]]
     (when wrong-password?
       [components/tooltip (i18n/label :t/wrong-password)])]))

;; Cancel and Sign buttons, signing with password
(defview signing-buttons [cancel-handler sign-handler]
  (letsubs [sign-enabled? [:wallet.send/sign-password-enabled?]]
    [react/view wallet.styles/buttons-container
     [react/touchable-highlight {:on-press cancel-handler}
      [react/view (wallet.styles/button-container true)
       [components/button-text (i18n/label :t/cancel)]]]
     [react/view components.styles/flex]
     [react/touchable-highlight {:on-press sign-handler}
      [react/view (wallet.styles/button-container sign-enabled?)
       [components/button-text (i18n/label :t/transactions-sign-transaction)]
       [vi/icon :icons/forward {:color :white :container-style wallet.styles/forward-icon-container}]]]]))

;; Sign Later and Sign buttons
(defview sign-buttons [sign-later-handler]
  (letsubs [sign-enabled? [:wallet.send/sign-enabled?]]
    [react/view wallet.styles/buttons-container
     (when sign-enabled?
       [react/touchable-highlight {:on-press sign-later-handler}
        [react/view (wallet.styles/button-container sign-enabled?)
         [components/button-text (i18n/label :t/transactions-sign-later)]]])
     [react/view components.styles/flex]
     [react/touchable-highlight {:on-press (when sign-enabled? #(re-frame/dispatch [:set-in [:wallet/send-transaction :signing?] true]))}
      [react/view (wallet.styles/button-container sign-enabled?)
       [components/button-text (i18n/label :t/transactions-sign-transaction)]
       [vi/icon :icons/forward {:color :white :container-style wallet.styles/forward-icon-container}]]]]))

(defview send-transaction []
  (letsubs [amount       [:get-in [:wallet/send-transaction :amount]]
            amount-error [:get-in [:wallet/send-transaction :amount-error]]
            signing?     [:get-in [:wallet/send-transaction :signing?]]
            to-address   [:get-in [:wallet/send-transaction :to-address]]
            to-name      [:get-in [:wallet/send-transaction :to-name]]]
    [react/keyboard-avoiding-view wallet.styles/wallet-modal-container
     [react/view components.styles/flex
      [status-bar/status-bar {:type :wallet}]
      [toolbar-view signing?]
      [react/scroll-view {:keyboardShouldPersistTaps :always}
       [react/view components.styles/flex
        [react/view wallet.styles/choose-participant-container
         [components/choose-recipient {:address  to-address
                                       :name     to-name
                                       :on-press #(re-frame/dispatch [:navigate-to :choose-recipient])}]]
        [react/view wallet.styles/choose-wallet-container
         [components/choose-wallet]]
        [react/view wallet.styles/amount-container
         [components/amount-input
          {:error         amount-error
           :input-options {:auto-focus     true
                           :default-value  amount
                           :on-change-text #(re-frame/dispatch [:wallet/set-and-validate-amount %])}}]
         [react/view wallet.styles/choose-currency-container
          [components/choose-currency wallet.styles/choose-currency]]]]]
      [components/separator]
      (if signing?
        [signing-buttons
         #(re-frame/dispatch [:wallet/discard-transaction])
         #(re-frame/dispatch [:wallet/sign-transaction])]
        [sign-buttons sign-later])
      (when signing?
        [sign-panel])]]))

(defn toolbar-modal []
  [toolbar/toolbar2 {:style wallet.styles/toolbar}
   [toolbar/nav-button (act/close-white act/default-handler)]
   [toolbar/content-title {:color :white} (i18n/label :t/send-transaction)]])

(defview send-transaction-modal []
  (letsubs [amount       [:get-in [:wallet/send-transaction :amount]]
            amount-error [:get-in [:wallet/send-transaction :amount-error]]
            signing?     [:get-in [:wallet/send-transaction :signing?]]
            to-address   [:get-in [:wallet/send-transaction :to-address]]
            to-name      [:get-in [:wallet/send-transaction :to-name]]
            recipient    [:contact-by-address @to-name]]
    [react/keyboard-avoiding-view wallet.styles/wallet-modal-container
     [react/view components.styles/flex
      [status-bar/status-bar {:type :wallet}]
      [toolbar-modal]
      [react/scroll-view {:keyboardShouldPersistTaps :always}
       [react/view components.styles/flex
        [react/view wallet.styles/choose-participant-container
         [components/choose-recipient-disabled {:address  to-address
                                                :name     (:name recipient)}]]
        [react/view wallet.styles/choose-wallet-container
         [components/choose-wallet]]
        [react/view wallet.styles/amount-container
         [components/amount-input-disabled amount]
         [react/view wallet.styles/choose-currency-container
          [components/choose-currency wallet.styles/choose-currency]]]]]
      [components/separator]
      (if signing?
        [signing-buttons
         #(re-frame/dispatch [:wallet/cancel-signing-modal])
         #(re-frame/dispatch [:wallet/sign-transaction-modal])]
        [sign-buttons #(re-frame/dispatch [:navigate-back])])
      (when signing?
        [sign-panel])]]))