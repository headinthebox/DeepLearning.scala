package com.thoughtworks.deeplearning

import com.thoughtworks.sde.core.MonadicFactory
import resource.{ManagedResource, Resource}

import scalaz.Monad

/**
  * @author 杨博 (Yang Bo) &lt;pop.atry@gmail.com&gt;
  */
object raii extends MonadicFactory.WithTypeClass[Monad, ManagedResource] {
  implicit override object typeClass extends Monad[Resource] {

    override def bind[A, B](fa: ManagedResource[A])(f: (A) => ManagedResource[B]): ManagedResource[B] = {
      fa.flatMap(f)
    }

    override def point[A](a: => A): Resource[A] = {
      resource.managed(a)
    }
  }
}
