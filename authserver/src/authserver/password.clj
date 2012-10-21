(ns authserver.password
  (:import [java.security MessageDigest]))

(def md (MessageDigest/getInstance "SHA-1"))

(defn ascii2hex [ascii-str]
  (map #(Integer/parseInt % 16) (map #(apply str %) (partition 2 ascii-str))))

(defn bytes2hexStr [bytes]
  (apply str
         (map #(.toUpperCase %)
              (map #(format "%02x" %)
                   (map #(bit-and 0xFF %) (seq bytes))))))

(defn get-next-password [key-str]
  (. md reset)
  (bytes2hexStr
   (seq
    (. md digest
       (into-array Byte/TYPE
                   (map #(.byteValue %)
                        (ascii2hex key-str)))))))
    
