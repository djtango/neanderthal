(ns uncomplicate.neanderthal.opencl-test
  (:require [midje.sweet :refer :all]
            [uncomplicate.neanderthal
             [core :refer :all]
             [native :refer :all]
             [math :refer [pow]]
             [opencl :refer [clv clge]]
             [opencl-amd-gcn :refer :all]]
            [uncomplicate.clojurecl
             [core :refer [with-default with-release *context* *command-queue*
                           finish!]]]))

(def cnt (long (+ 1000  (pow 2 25))))
(def x-magic 2)
(def y-magic 5)

(facts
 "RealVector methods"
 (with-default
   (let [host-x (doto (sv cnt) (entry! x-magic))
         host-y (doto (sv cnt) (entry! y-magic))]
     (with-release [engine (gcn-single *context* *command-queue*)
                    cl-x (clv engine cnt)
                    cl-y (clv engine cnt)]

       (dim cl-x) => cnt

       (entry! cl-x x-magic)
       (entry! cl-y y-magic)
       (entry! host-x 6 100000.0)
       (write! cl-x host-x)

       (float (dot cl-x cl-y)) => (float (dot host-x host-y))

       (float (asum cl-x))
       => (float (+ 100000 (* (double x-magic) (double cnt))))

       (nrm2 cl-x) => (roughly (nrm2 host-x))

       (iamax cl-x) => 6

       (read! (scal! 2 cl-x) (sv cnt)) => (scal! 2 host-x)

       (read! (axpy! 2 cl-x cl-y) (sv cnt))
       => (axpy! 2 host-x host-y)))))


(facts
 "Carrier methods"
 (with-default
   (let [host-x (doto (sv cnt) (entry! 3.5))
         host-y (doto (sv cnt) (entry! 1.1))]
     (with-release [engine (cl-engine *command-queue*)
                    cl-x (clv engine cnt)
                    cl-y (clv engine cnt)]

       (write! cl-x host-x) => cl-x
       (write! cl-y host-y) => cl-y

       (with-release [cl-zero (zero cl-x)]
         (read! cl-zero (sv cnt))) => (sv cnt)

         (swp! cl-x cl-y) => cl-x
         (swp! cl-x cl-y) => cl-x

         (read! cl-x (sv cnt)) => host-x

         (copy! cl-x cl-y) => cl-y

         (read! cl-y host-y) => host-x))))

(facts
 "Real matrix-vector multiplication."
 (with-default
   (let [m-cnt 2050
         n-cnt 337
         a-magic 3
         x-magic 2
         y-magic 5
         host-a (doto (sge m-cnt n-cnt) (entry! a-magic))
         host-x (doto (sv n-cnt) (entry! x-magic))
         host-y (doto (sv m-cnt) (entry! y-magic))]
     (with-release [engine (cl-engine *command-queue*)
                    cl-a (clge engine m-cnt n-cnt)
                    cl-x (clv engine n-cnt)
                    cl-y (clv engine m-cnt)]

       (entry! cl-a a-magic)
       (entry! cl-x x-magic)
       (entry! cl-y y-magic)

       (read! (mv! cl-y 10 cl-a cl-x 100) (sv m-cnt))
       => (mv! host-y 10 host-a host-x 100)))))

(facts
 "Real matrix-matrix multiplication."
 (let [m-cnt 4096
       k-cnt 4096
       n-cnt 4096
       host-a (sge m-cnt k-cnt (range (* m-cnt k-cnt)))
       host-b (sge k-cnt n-cnt (map (partial * 2) (range (* m-cnt k-cnt))))
       host-c (sge m-cnt n-cnt (map (partial * 2) (range (* m-cnt n-cnt))))]
   (with-default
     (with-release [engine (cl-engine *command-queue*)
                    cl-a (clge engine m-cnt k-cnt)
                    cl-b (clge engine k-cnt n-cnt)
                    cl-c (clge engine m-cnt n-cnt)]

       (write! cl-a host-a)
       (write! cl-b host-b)
       (write! cl-c host-c)

       (time (do (mm! cl-c 10 cl-a cl-b 100) (finish! *command-queue*)))
       ;;=> (time (mm! host-c 10 host-a host-b 100))
       ))))
