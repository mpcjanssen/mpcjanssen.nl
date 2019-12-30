(defproject cryogen "0.1.0"
            :description "The knight who say NIH!"
            :url "https://mpcjanssen.nl"
            :license {:name "Eclipse Public License"
                      :url "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.10.0"]
                           [ring/ring-devel "1.7.1"]
                           [compojure "1.6.1"]
                           ;; [cryogen-markdown "0.1.11"]
                           ;; [cryogen-markdown-diagram "0.0.1"]
                           [cryogen-asciidoctor-diagram "0.0.1"]
                           [ring-server "0.5.0"]
                           [cryogen-core "0.2.3"]]
            :plugins [[lein-ring "0.12.5"] [lein-sub "0.2.4"]]
            :main cryogen.core
            :sub ["cryogen-markdown-diagram"
                  "cryogen-asciidoctor-diagram"]
            :ring {:init cryogen.server/init
                   :handler cryogen.server/handler})
