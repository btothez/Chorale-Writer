(ns dumbsound.core
  ; (:require [clojure.core.async
  ;         :as async
  ;         :refer :all)
  (:import [javax.sound.midi MidiSystem Sequencer]
           [jline.console ConsoleReader] )
  (:gen-class))

; (use 'dumbsound.core :reload-all)

(def synth (doto (MidiSystem/getSynthesizer) .open))
(def instruments (.. synth getDefaultSoundbank getInstruments))
(def instrument (nth instruments 78))
(def channel (aget (.getChannels synth) 0))
(def scalemap {:1 0 :2 2 :3 4 :4 5 :5 7 :6 9 :7 11})
(.loadInstrument synth instrument)
(.programChange channel 78)

(defn iter-chord [chord]
  ((reduce
    (fn [acc new]
      (let [{:keys [ongoing last octave]} acc
            lower (or octave (< new last))]
      {:ongoing
        (conj ongoing (if lower (+ 12 new) new))
       :last new
       :octave lower}))
    {:ongoing [] :last 0 :octave false} chord) :ongoing))


(defn playnote [channel note]
  (. channel noteOn note 127)
  (Thread/sleep 600)
  (. channel noteOff note))

(defn playnotes [channel notes]
  (doseq [note notes]
    (. channel noteOn note 127))
  (Thread/sleep 600)
  (doseq [note notes]
    (. channel noteOff note)))

(defn mapnote [note scalemap start]
  (if (< note 0)
    (+ (- start 12) (get scalemap (keyword (str (Math/abs note)))))
    (+ start (get scalemap (keyword (str note))))
  ))

(defn playchord
  [channel chord start]
  (playnotes
     channel
     (iter-chord (map #(mapnote % scalemap 60)
      chord))))

(defn play [sequence]
  (doseq [thing sequence]
    (println thing (type thing))
    (if (instance? Long thing)
      (playnote channel (mapnote thing scalemap 60))
      (playchord channel thing 60))))


; LEIN TRAMPOLINE RUN
(defn -main []
  (while true (do
    (playnote channel
            (mod
               (.readCharacter (ConsoleReader.))
                60)))))



(defn getSynthPlayer [channelNbr instrumentNbr]
  "Initialize synthesizer and return play function"
  (let [synth (javax.sound.midi.MidiSystem/getSynthesizer)]
    (do
      (.open synth) ; Open synth before using
        (let [channels (.getChannels synth)
              instruments (.. synth getDefaultSoundbank getInstruments)]
          (do
            (let [channel (nth channels channelNbr)
                  instrument (nth instruments instrumentNbr)]
              (println "Instrument" instrumentNbr "is" (.getName instrument))
              (.loadInstrument synth instrument)
              (.programChange channel instrumentNbr) ; Lots of blogs never mentioned this!
              (fn [volume note duration] ; play function
                (do
                  (.noteOn channel note volume)
                  (Thread/sleep duration)
                  (.noteOff channel note)))))))))

; (while true (play [[1 5 1 3] [1 5 1 3] [1 5 1 3] [1 6 1 4] [1 7 1 5] [1 7 1 5] [1 6 1 4] [1 6 1 4] [1 5 1 3] [4 4 6 2] [5 3 5 1] [5 2 5 7] [1 1 3 1] [1 1] 1 1 [-7 5 2 4] [1 5 1 3] [-6 4 1 4] [-7 4 7 2] [-5 3 5 3][-5 3 5 3] [-6 3 6 1]  [-6 3 6 1] [-4 4 6 2] [-4 4 6 2] [-5 3 5 1] [-5 2 5 7] [1 3 5 1] 1 1 1]))
; keys - destructure the hash input OR give it something else
; (. channel noteOn c velocity)

; (defn play-note [synth channel note-map]
;   (let [{:keys [note velocity duration]
;          :or {note 60
;               velocity 127
;               duration 1000}} note-map]
;     (. channel noteOn note velocity)
;     (Thread/sleep duration)
;     (. channel noteOff note)))

; (defn play-notes
;   [notes]
;   (with-open [synth (doto (MidiSystem/getSynthesizer) .open)]
;     (let [channel (aget (.getChannels synth) 0)]
;       (doseq [note notes
;         (play-note synth channel note)))))

; (filter #(midi-device? %1) (midi-devices))
; (doseq [x [1 2 4 5]] (println x))
; (defn play-c-major-scale []
;   (let [c-major [0 2 4 5 7 9 11 12]
;         c-major-notes (map #(hash-map :note (+ % 60) :duration 400) c-major)]
;       (concat c-major-notes
;           (rest (reverse c-major-notes)))))

;     (play-notes (concat c-major-notes
;                         (rest (reverse c-major-notes))))))

; #(hash-map :note (+ % 60) :duration 400)
; doto (does the thing and then uses it as arg for following function)
; with-open [bindings] &body
; aget list index
; hash-map

; (defn -main
;   "I don't do a whole lot ... yet."
;   [& args]
;   (println "HERE WE GO....")
;   (println (play-c-major-scale)))

  ; https://github.com/rosejn/midi-clj/blob/master/src/midi.clj

  ; (import [java.sound.midi])
  ; (import '(javax.sound.midi MidiSystem Synthesizer))
  ; (defn play-note [synth channel note-map]
  ;   (let [{:keys [note velocity duration]
  ;          :or {note 60
  ;               velocity 127
  ;               duration 1000}} note-map]
  ;     (. channel noteOn note velocity)
  ;     (Thread/sleep duration)
  ;     (. channel noteOff note)))
  ; (defn play-notes
  ;   [notes]
  ;   (with-open [synth (doto (MidiSystem/getSynthesizer) .open)]
  ;     (let [channel (aget (.getChannels synth) 0)]
  ;       (doseq [note notes]
  ;         (play-note synth channel note)))))
  ; (defn play-c-major-scale []
  ;   (let [c-major [0 2 4 5 7 9 11 12]
  ;         c-major-notes (map #(hash-map :note (+ % 60) :duration 400) c-major)]
  ;     (play-notes (concat c-major-notes
  ;                         (rest (reverse c-major-notes))))))
  ; (play-c-major-scale)
  ; (play-c-major-scale))
  ; (play-c-major-scale)
  ; (ns dumbsound.core
  ;   (:import (javax.sound.midi MidiSystem System))
  ;   (:gen-class))
  ; (defn play-note [synth channel note-map]
  ;   (let [{:keys [note velocity duration]
  ;          :or {note 60
  ;               velocity 127
  ;               duration 1000}} note-map]
  ;     (. channel noteOn note velocity)
  ;     (Thread/sleep duration)
  ;     (. channel noteOff note)))
  ; (defn play-notes
  ;   [notes]
  ;   (with-open [synth (doto (MidiSystem/getSynthesizer) .open)]
  ;     (let [channel (aget (.getChannels synth) 0)]
  ;       (doseq [note notes]
  ;         (play-note synth channel note)))))
  ; (defn play-c-major-scale []
  ;   (let [c-major [0 2 4 5 7 9 11 12]
  ;         c-major-notes (map #(hash-map :note (+ % 60) :duration 400) c-major)]
  ;     (play-notes (concat c-major-notes
  ;                         (rest (reverse c-major-notes))))))
  ; (ns dumbsound.core
  ;   (:import (javax.sound.midi MidiSystem System))
  ;   (:gen-class))
  ; (defn play-note [synth channel note-map]
  ;   (let [{:keys [note velocity duration]
  ;          :or {note 60
  ;               velocity 127
  ;               duration 1000}} note-map]
  ;     (. channel noteOn note velocity)
  ;     (Thread/sleep duration)
  ;     (. channel noteOff note)))
  ; (defn play-notes
  ;   [notes]
  ;   (with-open [synth (doto (MidiSystem/getSynthesizer) .open)]
  ;     (let [channel (aget (.getChannels synth) 0)]
  ;       (doseq [note notes]
  ;         (play-note synth channel note)))))
  ; (defn play-c-major-scale []
  ;   (let [c-major [0 2 4 5 7 9 11 12]
  ;         c-major-notes (map #(hash-map :note (+ % 60) :duration 400) c-major)]
  ;     (play-notes (concat c-major-notes
  ;                         (rest (reverse c-major-notes))))))
  ; (play-c-major-scale)
  ; (defn play-note [synth channel note-map]
  ;   (let [{:keys [note velocity duration]
  ;          :or {note 60
  ;               velocity 127
  ;               duration 1000}} note-map]
  ;     (. channel noteOn note velocity)
  ;     (Thread/sleep duration)
  ;     (. channel noteOff note)))
  ; (defn play-notes
  ;   [notes]
  ;   (with-open [synth (doto (MidiSystem/getSynthesizer) .open)]
  ;     (let [channel (aget (.getChannels synth) 0)]
  ;       (doseq [note notes]
  ;         (play-note synth channel note)))))
  ; (defn play-c-major-scale []
  ;   (let [c-major [0 2 4 5 7 9 11 12]
  ;         c-major-notes (map #(hash-map :note (+ % 60) :duration 400) c-major)]
  ;     (play-notes (concat c-major-notes
  ;                         (rest (reverse c-major-notes))))))
  ; (play-c-major-scale)
  ; play-note
  ; play-notes
  ; play-c-major-scale
  ; (play-c-major-scale )
  ; (play-c-major-scale)
  ; (-main)
  ; (import '(javax.sound.midi MidiSystem Synthesizer))
  ; *1
  ; *e
  ; (def cmaj [0 1 3 5 7 8 10 12])
  ; cmaj
  ; #(def phryg (map ))
  ; {:note 60 :duration 400}
  ; (def c {:note 60 :duration 400})
  ; c
  ; (doto (MidiSystem/getSynthesizer)
  ; )
  ; MidiDevice
  ; (MidiDevice/getSynthesizer)
  ; MidiDevice/
  ; ?
  ; MidiDevice
  ; MidiDevice/getSynthesizer
  ; {:note 60 :duration 400}
  ; (def note {:note 60 :duration 400})
  ; note
  ; (MidiDevice)
  ; (MidiDevice/getReceivers)
  ; (doto (MidiDevice/getReceivers) .open)
  ; (doto (MidiDevice/getReceivers) open)
  ; (doto (MidiDevice/getReceivers))
  ; (doto (MidiDevice/getInfo))
  ; (doto (MidiDevice/getInfo) .getInfo)
  ; (doto (MidiDevice/getInfo) .open)
  ; (doto (MidiDevice/Info) .getInfo)
  ; MidiDevice
  ; MidiDevice/getSynthesizer
  ; MidiDevice/getInfo
  ; MidiDevice/Info
  ; (MidiDevice/Info)
  ; (MidiDevice/open)
  ; (import '((javax.sound.midi Sequencer Synthesizer))
  ;                        MidiSystem MidiDevice Receiver Transmitter MidiEvent
  ;                        MidiMessage ShortMessage SysexMessage
  ; (import '(javax.sound.midi MidiSystem))
  ; (MidiSystem/getSynthesizer)
  ; (doto (MidiSystem/getSynthesizer) .open)
  ; (def synth (doto (MidiSystem/getSynthesizer) .open))
  ; synth
  ; (MidiSystem/getMidiDeviceInfo)
  ; (println MidiSystem/getMidiDeviceInfo)
  ; (MidiSystem/getMidiDeviceInfo)
  ; MidiSystem/getMidiDeviceInfo
  ; (MidiSystem/getMidiDeviceInfo)
  ; (defn midi-devices []
  ;   "Get all of the currently available midi devices."
  ;   (for [info (MidiSystem/getMidiDeviceInfo)]
  ;     (let [device (MidiSystem/getMidiDevice info)]
  ;       (with-meta
  ;         {:name         (.getName info)
  ;          :description  (.getDescription info)
  ;          :vendor       (.getVendor info)
  ;          :version      (.getVersion info)
  ;          :sources      (.getMaxTransmitters device)
  ;          :sinks        (.getMaxReceivers device)
  ;          :info         info
  ;          :device       device}
  ;         {:type :midi-device}))))
  ; midi-devices
  ; (midi-devices)
  ; (def devices (midi-devices))
  ; devices
  ; (first devices)
  ; (:name (first devices))
  ; (defn midi-ports
  ;   "Get the available midi I/O ports (hardware sound-card and virtual ports)."
  ;   []
  ;   (filter #(and (not (instance? Sequencer   (:device %1)))
  ;                 (not (instance? Synthesizer (:device %1))))
  ;           (midi-devices)))
  ; (import '(javax.sound.midi Synthesizer))
  ; (defn midi-ports
  ;   "Get the available midi I/O ports (hardware sound-card and virtual ports)."
  ;   []
  ;   (filter #(and (not (instance? Sequencer   (:device %1)))
  ;                 (not (instance? Synthesizer (:device %1))))
  ;           (midi-devices)))
  ; midi-portx
  ; midi-ports
  ; midi-devices
  ; (midi-devices)
  ; (:name (first devices))
  ; (:device (first devices))
  ; instance? Synthesizer (:device (first devices))
  ; (instance? Synthesizer (:device (first devices)))
  ; (instance? Sequencer (:device (first devices)))
  ; (midi-ports)
  ; (defn midi-sinks
  ;   "Get the midi output sinks."
  ;   []
  ;   (filter #(not (zero? (:sinks %1))) (midi-ports)))
  ; (midi-sinks)
  ; (defn midi-device?
  ;   "Check whether obj is a midi device."
  ;   [obj]
  ;   (= :midi-device (type obj)))
  ; (filter  #(midi-device?) midi-devices)
  ; (filter  #(midi-device?) (midi-devices))
  ; (midi-device)
  ; (midi-devices)
  ; (first (midi-devices))
  ; (type (first (midi-devices)))
  ; (midi-device? (first (midi-devices))))
  ; (midi-device? (first (midi-devices)))
  ; (midi-device? (midi-devices))
  ; (filter #(midi-device?) (midi-devices))
  ; (filter #(midi-device?) midi-devices)
  ; (filter #(midi-device?) (midi-devices))
  ; (filter #midi-device? (midi-devices))
  ; (filter #(midi-device? %1) (midi-devices))
  ; note
  ; (MidiSystem/getSynthesizer)
  ; MidiSystem/getSynthesizer
  ; synth
  ; (def synth (doto (MidiSystem/getSynthesizer) .open))
  ; synth
  ; (.getChannels)
  ; (.getChannels syth)
  ; (with-open [synth (doto (MidiSystem/getSynthesizer) .open))]
  ; (with-open [synth (doto (MidiSystem/getSynthesizer) .open)]
  ; (def note {:note 60 :duration 400})
  ; note
  ; (note)
  ; '(note)
  ; (doseq "ansdf")
  ; (doseq [1 2 3 5])
  ; (doseq (1 2 3 5))
  ; (doseq x [1 2 3 5])
  ; (doseq x [1 2 3 5] x)
  ; (doseq x [1 2 3 5] (x))
  ; (doseq x [1 2 3 5] (println x))
  ; (doseq [x (1 2 4 5)] (println x))
  ; (doseq [x (1 2 4 5)] x)
  ; (doseq [x (1 2 4 5)] (println x))
  ; (doseq [x [1 2 4 5]] (println x))
  ; synth
  ; (def synth (doto (MidiSystem/getSynthesizer) .open))
  ; (import '(javax.sound.midi Synthesizer MidiSystem))
  ; (def synth (doto (MidiSystem/getSynthesizer) .open))
  ; synth
  ; (.getChannels synth)
  ; (aget (.getChannels synth) 0)
  ; (def channel (aget (.getChannels synth) 0)0
  ; (def channel (aget (.getChannels synth) 0))
  ; channel
  ; note
  ; synth
  ; (:keys note)
  ; (def client {:name "Super Co."
  ;              :location "Philadelphia"
  ;              :description "The worldwide leader in plastic tableware."})
  ; client
  ; (let [{:keys [name location description]} client]
  ;   (println name location "-" description))
  ; client
  ; (def c 60)
  ; (def velocity 127)
  ; (def duration 1000)
  ; channel
  ; (. channel noteOn note velocity)
  ; channel
  ; noteOn
  ; note
  ; (. channel noteOn c velocity)
  ; (. channel noteOff c )
  ; (. channel noteOn c velocity)
  ; (. channel noteOff c )
  ; (. channel noteOn c velocity)
  ; (. channel noteOff c )
  ; (. channel noteOn c velocity)
  ; (. channel noteOn c (+ 10 elocity))
  ; (. channel noteOn c (+ 10 velocity))
  ; (. channel noteOn c velocity)
  ; (. channel noteOn c (+ 10 velocity))
  ; (. channel noteOff c )
  ; c
  ; (def phryg [0 1 3 5 7 8 10 12])
  ; (map #(hash-map (+ % 60)) phryg)
  ; (map #(+ % 60) phryg)
  ; doseq [note (map #(+ % 60) phryg)] (println note))
  ; (doseq [note (map #(+ % 60) phryg)] (println note))
  ; ; (doseq [note (map #(+ % 60) phryg)] (println note))
  ; channel
  ; defn play []
  ; (defn play []
  ; (doseq [note (map #(+ % 60) phryg)] (println note)))
  ; play
  ; (play)
  ; (defn play [note] (. channel noteOn note 127))
  ; (play 60)
  ; (defn playseq [notes]
  ; ((doseq [note notes] ((play note)(Thread/sleep 400))))
  ; )_
  ; (defn playseq [notes]
  ; (doseq [note notes] ((play note)(Thread/sleep 400))
  ; )
  ; (playseq phryg)
  ; phyrg
  ; phryg
  ; (playseq phryg)
  ; (play (first (phyrg)))
  ; (play (first (phryg)))
  ; (play (first phryg))
  ; (first phryg)
  ; (doseq [note (map #(+ % 60) phryg)] (println note)))
  ; (defn playseq [notes]
  ; (defn playseq [start notes]
  ; (play (+ 10 (first phryg)))
  ; (play (+ 60 (first phryg)))
  ; (defn playnotes [notes]
  ; (doseq [note notes] (play note)))
  ; (playnotes [60 61 60])
  ; (playnotes [60 64])
  ; (playnotes [60 64 67])
  ; (playnotes [60 61 64])
  ; (playnotes [60 64 67])
  ; (playnotes [60 61 64])
  ; (playnotes [60 64 67])
  ; (playnotes [60 61 64])
  ; (playnotes [60 61 68])
  ; (playnotes [60 61 64])
  ; (playnotes [60 64 67])
  ; (playnotes [60 61 64])
  ; (playnotes [60 64 67])
  ; (playnotes [60 61 68])
  ; (playnotes [60 61 64])
  ; (playnotes [60 64 67])
  ; (playseq [notes]
  ; (doseq [note notes] ((play note)(Thread/sleep 400))))
  ; (playseq [notes]
  ; (println notes))
  ; (playseq [notes]
  ; (println notes))
  ; (defn playseq [notes]
  ; (println notes))
  ; (playseq pattern)
  ; (playseq phyrg)
  ; (playseq phryg)
  ; (defn playseq [notes]
  ; (doseq [note notes] ((play note)(Thread/sleep 400))))
  ; (playseq phryg)
  ; (map #(+ 60 %) phryg)
  ; (playseq (map #(+ 60 %) phryg))
  ; (playnotes (map #(+ 60 %) phryg))
  ; (defn playseq [notes]
  ; (doseq [note notes] ((println note))
  ; )
  ; (defn playseq [notes]
  ; (doseq [note notes] (println note)))
  ; (playseq [ 1 32 4])
  ; (doseq [note notes] (println note) (Thread/sleep 100)))
  ; (doseq [note notes] (println note) (Thread/sleep 100))))
  ; (defn playseq [notes]
  ; (doseq [note notes] (println note) (Thread/sleep 100))))
  ; (doseq [note notes] (println note) (Thread/sleep 100)))
  ; (defn playseq [notes]
  ; (doseq [note notes] (println note) (Thread/sleep 100)))
  ; (playseq [ 1 32 4])
  ; (defn playseq [notes]
  ; (doseq [note notes] (println note) (Thread/sleep 400)))
  ; (playseq [ 1 32 4])
  ; (defn playseq [notes]
  ; (doseq [note notes] (play note) (Thread/sleep 400)))
  ; (playseq [ 60 32 4])
  ; (playseq [ 60 59 60])
  ; (playseq (map #(+ 60 %) phryg))
  ; (playnotes (map #(+ 60 %) phryg))
  ; (playseq (map #(+ 60 %) phryg))
  ; (playnotes (map #(+ 60 %) phryg))
  ; (playseq (map #(+ 60 %) phryg))
  ; (def phrygdom [0 1 4 5 7 8 11 12])
  ; (playseq (map #(+ 60 %) phrygdom))
  ; (def phrygdom [0 1 4 5 7 8 11 12])
  ; (playseq (map #(+ 60 %) phryg))
  ; (def doresque [0 2 3 5 7 8 11 12])
  ; (def phrygdor [0 1 3 5 7 9 10 12])
  ; (playseq (map #(+ 60 %) phrygdor))
  ; (def phrygdor [0 1 3 5 7 9 11 12])
  ; (playseq (map #(+ 60 %) phrygdor))
  ; (def phrygdor [0 1 3 5 7 8 11 12])
  ; (playseq (map #(+ 60 %) phrygdor))
  ; (def phrygdor [0 1 3 5 7 9 11 12])
  ; (playseq (map #(+ 60 %) phrygdor))
  ; (defn makenotes [pattern]
  ; (map #(+ 60 %) pattern))
  ; (playseq (makenotes phrygdor))
  ; (playnotes (makenotes phrygdor))
  ; (playseq (makenotes phrygdor))
  ; (rev phrygdor)
  ; (reverse phrygdor)
  ; (rest (reverse phrygdor))
  ; (concat phryg (rest (reverse phrygdor)))
  ; (playseq (concat phryg (rest (reverse phrygdor))))
  ; (playseq (makenotes (concat phryg (rest (reverse phrygdor)))))
  ; (playseq (makenotes (concat phrygdor (rest (reverse phryg)))))
