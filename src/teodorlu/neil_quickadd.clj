(ns teodorlu.neil-quickadd
  (:require
   [babashka.cli :as cli]
   [babashka.fs :as fs]
   [babashka.process :as process]
   [clojure.edn :as edn]
   [clojure.string :as str]))

;; workaround for https://github.com/teodorlu/neil-quickadd/issues/1 and https://github.com/babashka/fs/issues/89
(require '[babashka.fs] :reload)

;; Rationale
;;
;; `neil-quickadd` gets you started with your new Clojure project faster.
;; Add the dependencies you usually use in seconds!
;;
;; There are two important commands:
;;
;;    neil-quickadd scan DIR   ; Traverses DIR to index all your dependencies
;;    neil-quickadd            ; from your quickadd index, select a dep with
;;                              `fzf` and add it with `neil dep add`
;;
;; If you want to script around `neil-quickadd`, also consider:
;;
;;    neil-quickadd libs          ; list indexed libs

(defn ^:private safely-slurp-edn [path orelse]
  (try (edn/read-string (slurp path))
       (catch Exception _ orelse)))

(defn ^:private scan-deps-files
  ([path] (scan-deps-files path nil))
  ([path {:keys [max-depth]}]
   (let [deps-files (map str (fs/glob path "**/deps.edn" (when max-depth {:max-depth max-depth})))
         safe-read-deps (fn [deps-file]
                          (or (seq (keys (:deps (safely-slurp-edn deps-file {}))))
                              (list)))]
     (->> (mapcat safe-read-deps deps-files)
          (into #{})
          sort))))

(defn ^:private index-file-path []
  (str (fs/expand-home "~/.local/share/neil-quickadd") "/index.edn"))

(defn ^:private update-index [k f & args]
  (let [m (safely-slurp-edn (index-file-path) {})
        m (if (map? m) m {})]
    (fs/create-dirs (fs/expand-home "~/.local/share/neil-quickadd"))
    (spit (index-file-path)
          (pr-str (apply update m k f args)))))

(defn quickadd-scan [{:keys [opts]}]
  (when (or (:h opts) (:help opts))
    (println (str/trim "
Usage:

  neil-quickadd scan [OPTS...]
  neil-quickadd scan DIR [OPTS...]

Traverses DIR for dependencies. DIR defaults to the current directory.
Dependencies are added to the neil-quickadd index.

Allowed OPTS:

  --maxdepth N     ; Limits the traversal to N layers down from DIR.
"))
    (System/exit 0))
  (let [root-dir (:path opts ".")
        root-dir (-> root-dir fs/expand-home fs/absolutize str)
        {:keys [max-depth]} opts
        all-deps (scan-deps-files root-dir (when max-depth {:max-depth max-depth}))]
    (update-index root-dir (fn [_] all-deps))))

(defn quickadd-libs* []
  (when-let [libs (seq (apply concat (vals (safely-slurp-edn (index-file-path) {}))))]
    (sort (into #{} libs))))

(defn quickadd-libs [{:keys [opts]}]
  (when (or (:h opts) (:help opts))
    (println (str/trim "
Usage: neil-quicadd libs

List indexed libraries.
"))
    (System/exit 0))
  (if-let [libs (quickadd-libs*)]
    (println (str/join "\n" libs))
    (do (println "No libs indexed")
        (println "Please use `neil-quickadd scan` to populate the index")
        (System/exit 1))))

(defn quickadd-clear-index [{:keys [opts]}]
  (when (or (:h opts) (:help opts))
    (println (str/trim "
Usage: neil-quicadd clear-index

Deletes the index of scanned libraries.
"))
    (System/exit 0))
  (fs/delete-if-exists (index-file-path))
  nil)

(declare print-subcommands)

(defn ^:private neil-dep-versions [lib]
  (let [process-result (process/shell {:out :string}
                                      "neil" "dep" "versions" lib)]
    (when (= 0 (:exit process-result))
      (:out process-result))))

(defn ^:private neil-dep-add [& args]
  (apply process/shell "neil" "dep" "add" args))

(defn quickadd [{:keys [opts]}]
  (when (or (:h opts) (:help opts))
    (print-subcommands {})
    (System/exit 0))
  (if-let [libs (quickadd-libs*)]
    (loop []
      (let [fzf-result (process/shell {:out :string :in (str/join "\n" (into [":quit"] libs))} "fzf")]
        (when (not= 0 (:exit fzf-result))
          ;; If FZF terminates, we terminate.
          (System/exit 0))
        (let [selected (str/trim (:out fzf-result))]
          (when (= ":quit" selected)
            ;; Also provide a "non crashing" exit option.
            (System/exit 0))
          (if (:select-version opts)
            (let [versions-string (neil-dep-versions selected)
                  selected-process-result (process/shell {:out :string :in versions-string} "fzf")]
              (when (= 0 (:exit selected-process-result))
                (let [selected-tokenized (process/tokenize (:out selected-process-result))]
                  (prn (concat ["neil" "dep" "add"]
                               selected-tokenized))
                  (apply neil-dep-add selected-tokenized)
                 (recur))))
            (do
              (prn ["neil" "dep" "add" selected])
              (neil-dep-add selected)
              (recur)))
          )))
    (do (println "No libs indexed")
        (println "Please use `neil-quickadd scan` to populate the index")
        (System/exit 1))))

(declare dispatch-table)

(defn print-subcommands [{}]
  (println (str/trim "
Usage: neil-quickadd [COMMAND] [OPT...]

Available commands:

  neil-quickadd clear-index  ; Remove the index
  neil-quickadd help         ; Print subcommands
  neil-quickadd libs         ; Show the index
  neil-quickadd scan         ; Scan a folder for dependencies
  neil-quickadd              ; Add a dependency from the index with FZF
")))

(def dispatch-table
  [{:cmds ["clear-index"] :fn quickadd-clear-index}
   {:cmds ["help"]        :fn print-subcommands }
   {:cmds ["libs"]        :fn quickadd-libs     }
   {:cmds ["scan"]        :fn quickadd-scan      :args->opts [:path]}
   {:cmds []              :fn quickadd          }])

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
