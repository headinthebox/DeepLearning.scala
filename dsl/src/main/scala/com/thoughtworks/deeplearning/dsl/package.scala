package com.thoughtworks.deeplearning

import resource._
import com.thoughtworks.deeplearning.Layer._
import com.thoughtworks.deeplearning.dsl.layers.{Compose, Identity, Literal}

import language.implicitConversions

/**
  * @author 杨博 (Yang Bo) &lt;pop.atry@gmail.com&gt;
  */
package object dsl {

  /** @template */
  type BpAny = BackPropagationType[Any, _]

  implicit final class ToBatch[Data](a: Data) {
    def toBatch[Delta]: Batch.Aux[Data, Delta] = Literal[Data](a)
  }

  implicit def autoToLayer[A, Input <: Batch, OutputData, OutputDelta](a: A)(
      implicit toLayer: ToLayer.Aux[A, Input, OutputData, OutputDelta])
    : Layer.Aux[Input, Batch.Aux[OutputData, OutputDelta]] = {
    toLayer(a)
  }

  final class LayerOps[Input <: Batch, OutputData, OutputDelta](
      val toLiteral: Layer.Aux[Input, Batch.Aux[OutputData, OutputDelta]]) {

    def compose[G, NewInput <: Batch, InputData, InputDelta](g: G)(
        implicit differentiableType: ToLayer.Aux[G, NewInput, InputData, InputDelta],
        toInput: Layer.Aux[NewInput, Batch.Aux[InputData, InputDelta]] <:< Layer.Aux[NewInput, Input]
    ): Layer.Aux[NewInput, Batch.Aux[OutputData, OutputDelta]] = {
      Compose(toLiteral, toInput(differentiableType(g)))
    }

    def predict[InputData, InputDelta](inputData: InputData)(
        implicit ev: Layer.Aux[Input, Batch.Aux[OutputData, OutputDelta]] <:< Layer.Aux[
          Batch.Aux[InputData, InputDelta],
          Batch.Aux[OutputData, OutputDelta]]
    ): OutputData = {
      managed(toLiteral.forward(Literal[InputData](inputData))).acquireAndGet(_.value)
    }

    def train[InputData, InputDelta](inputData: InputData)(
        implicit ev: Layer.Aux[Input, Batch.Aux[OutputData, OutputDelta]] <:< Layer.Aux[
          Batch.Aux[InputData, InputDelta],
          Batch.Aux[OutputData, OutputDelta]],
        outputDataIsOutputDelta: OutputData <:< OutputDelta
    ): OutputData = {
      val outputBatch = toLiteral.forward(Literal[InputData](inputData))
      try {
        val loss = outputBatch.value
        outputBatch.backward(outputDataIsOutputDelta(loss))
        loss
      } finally {
        outputBatch.close()
      }

    }

  }

  implicit def toLayerOps[A, Input <: Batch, OutputData, OutputDelta](a: A)(
      implicit toLayer: ToLayer.Aux[A, Input, OutputData, OutputDelta]): LayerOps[Input, OutputData, OutputDelta] = {
    new LayerOps(toLayer(a))
  }

}
