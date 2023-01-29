;;; neil-quickadd.el -*- lexical-binding: t; -*-
;;
;; Copyright (C) 2023 Teodor Heggelund
;;
;; Author: Teodor Heggelund <teodorlu@teod-t490s>
;; Maintainer: Teodor Heggelund <teodorlu@teod-t490s>
;; Created: January 28, 2023
;; Modified: January 29, 2023
;; Version: 0.0.1
;; Keywords: abbrev bib c calendar comm convenience data docs emulations extensions faces files frames games hardware help hypermedia i18n internal languages lisp local maint mail matching mouse multimedia news outlines processes terminals tex tools unix vc wp
;; Homepage: https://github.com/teodorlu/neil-quickadd
;; Package-Requires: ((emacs "24.3"))
;;
;; This file is not part of GNU Emacs.
;;
;;; Commentary:
;;
;;
;;
;;; Code:

(require 'projectile)
(require 's)

(defun neil-quickadd ()
  (interactive)
  (let* ((libs (s-lines (s-trim (shell-command-to-string "neil-quickadd libs"))))
         (selected (completing-read "> " libs)))
    (projectile-run-shell-command-in-root (s-concat "neil dep add " selected))))

;; adding multiple after each other didn't feel right.
;;
;;  1. The deps.edn buffer doesn't update
;;  2. It's really fast to M-x neal-quickadd again.
;;
;; So keep this in the "box of potential ideas" for now.
;;
;; (defun neil-quickadd-multi ()
;;   (interactive)
;;   (while 't
;;     (let* ((libs (s-lines (s-trim (shell-command-to-string "neil-quickadd libs"))))
;;            (selected (completing-read "> " libs)))
;;       (projectile-run-shell-command-in-root (s-concat "neil dep add " selected)))))

(provide 'neil-quickadd)
;;; neil-quickadd.el ends here
