(ns teodorlu.neil-quickadd
  (:require
   [babashka.cli :as cli]
   [babashka.fs :as fs]
   [babashka.process :as process]
   [clojure.edn :as edn]
   [clojure.string :as str]))

;; Rationale
;;
;; `neil-quickadd` gets you started with your new Clojure project faster.
;; Add the dependencies you usually use in seconds!
;;
;; There are two important commands:
;;
;;    neil-quickadd scan PATH     ; Traverses PATH to find all your dependencies
;;    neil-quickadd               ; based on your quickadd index, add a dep, selecting dep with FZF
;;
;; If you want to script around `neil-quickadd`, also consider:
;;
;;    neil-quickadd libs          ; list up all indexed libs

(defn ^:private safe-read-edn-file [path orelse]
  (try (edn/read-string (slurp path))
       (catch Exception _ orelse)))

(defn ^:private scan-deps-files [path]
  (let [deps-files (map str (fs/glob path "**/deps.edn"))
        safe-read-deps (fn [deps-file]
                         (or (seq (keys (:deps (safe-read-edn-file deps-file {}))))
                             (list)))]
    (->> (mapcat safe-read-deps deps-files)
                      (into #{})
                      sort)))

(defn ^:private index-file-path []
  (str (fs/expand-home "~/.local/share/neil-quickadd") "/index.edn"))

(defn ^:private update-index [k f & args]
  (let [m (safe-read-edn-file (index-file-path) {})
        m (if (map? m) m {})]
    (fs/create-dirs (fs/expand-home "~/.local/share/neil-quickadd"))
    (spit (index-file-path)
          (pr-str (apply update m k f args)))))

(defn quickadd-scan [{:keys [opts]}]
  (let [path (:path opts ".")
        path (-> path fs/expand-home fs/absolutize str)
        all-deps (scan-deps-files path)]
    (update-index path (fn [_] all-deps))))

(defn quickadd-libs* []
  (when-let [libs (seq (apply concat (vals (safe-read-edn-file (index-file-path) {}))))]
    (sort (into #{} libs))))

(defn quickadd-libs [{}]
  (if-let [libs (quickadd-libs*)]
    (println (str/join "\n" libs))
    (do (println "No libs indexed")
        (println "Please use `neil-quickadd scan` to populate the index")
        (System/exit 1))))

(defn quickadd-clear-index [{}]
  (fs/delete-if-exists (index-file-path))
  nil)

(defn quickadd [{}]
  ;; TODO validation
  (if-let [libs (quickadd-libs*)]
    (let [selected (str/trim (:out (process/shell {:out :string :in (str/join "\n" libs)} "fzf")))]
      (prn ["neil" "dep" "add" selected])
      (process/shell "neil" "dep" "add" selected)
      nil ; for now, supress output and don't validate
      )
    (do (println "No libs indexed")
        (println "Please use `neil-quickadd scan` to populate the index")
        (System/exit 1))))

(declare dispatch-table)

(defn print-subcommands [{}]
  (println "usage: neil-quickadd <command>")
  (println "")
  (println "available commands:")
  (doseq [{:keys [cmds helptext]} dispatch-table]
    (let [helptext (if helptext (str "     ; " helptext)
                       "")]
      (println (str "  " "neil-quickadd " (str/join " " cmds) helptext)))))

(def dispatch-table
  [{:cmds ["clear-index"] :fn quickadd-clear-index}
   {:cmds ["help"]        :fn print-subcommands :helptext "Get help!"}
   {:cmds ["libs"]        :fn quickadd-libs     :helptext "Show the index"}
   {:cmds ["scan"]        :fn quickadd-scan     :helptext "Scan a folder for dependencies" :args->opts [:path]}
   {:cmds []              :fn quickadd          :helptext "Add a dependency from the index with FZF"}])

(defn ensure-env-ok
  "Terminate and give user error if the user needs to install something."
  []
  (when-not (fs/which "fzf")
    (println "neil-quickadd requires fzf.")
    (println "Please install fzf, then call `neil-quickadd` again.")
    (System/exit 1)))

(defn -main [& args]
  (ensure-env-ok)
  (cli/dispatch dispatch-table args))
