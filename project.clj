(defproject webradio "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [javazoom/jlayer "1.0.1"]
                 [clj-http "3.12.3"]
                 [org.slf4j/slf4j-nop "1.7.30"]
                 [org.apache.xmlgraphics/batik-anim "1.14"]
                 [org.apache.xmlgraphics/batik-svggen "1.14"]
                 [org.apache.xmlgraphics/batik-dom "1.14"]
                 [com.googlecode.juniversalchardet/juniversalchardet "1.0.3"]
                 [com.mpatric/mp3agic "0.9.1"]]
  :main webradio.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot [webradio.core]
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
