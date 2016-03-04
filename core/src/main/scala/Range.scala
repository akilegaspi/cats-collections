package dogs

import dogs.Predef._
import algebra.Order
import scala.annotation.tailrec

/**
  * Represent a range [x, y] that can be generated by using discrete operations
  */
sealed class Range[A](val start: A, val end: A) {

  /**
    * Calculate the difference with range.
    *
    * It returns a tuple with the difference to the right and to the left of range.
    *
    * It basically calculates what is to the left of range that is in this and what is to the right
    * of range that is in this (in both cases it does not include elements in range)
    */
  def -(range: Range[A])(implicit discrete: Enum[A], order: Order[A]): (Range[A], Range[A]) = {
    if (order.compare(end, range.start) < 0) {
      (this, Range.empty[A]())
    }
    else if (order.compare(start, range.end) > 0) {
      (Range.empty[A](), this)
    }
    else {
      (order.compare(start, range.start), order.compare(end, range.end)) match {
        case (0, x) if x > 0 => (Range.empty(), Range(discrete.succ(range.end), end))
        case (0, _)  => (Range.empty(), Range.empty())

        case (x, y) if x > 0 && y > 0 => (Range.empty(), Range(discrete.succ(range.end), end))
        case (x, _) if x > 0 => (Range.empty(), Range.empty())

        case (x, y) if x < 0 && y > 0 => (Range(start, discrete.pred(range.start)), Range(discrete.succ(range.end), end))
        case (x, _) if x < 0 => (Range(start, discrete.pred(range.start)), Range.empty())
      }
    }
  }

  /**
    * Verify that the passed range is within
    */
  def contains(range: Range[A])(implicit order: Order[A]) =
     order.lteqv(start, range.start) && order.gteqv(end, range.end)

  /**
    * Generates the elements of the range [start, end] base of the discrete operations
    */
  def generate(implicit discrete: Enum[A], order: Order[A]): List[A] = gen (start, end, List.empty)(_=>{})

  /**
    * Returns range [end, start]
    */
  def reverse(implicit discrete: Enum[A], order: Order[A]): Range[A] = Range(end, start)

  /**
    * Verify is x is in range [start, end]
    */
  def contains(x: A)(implicit A: Order[A]) = A.gteqv(x, start) && A.lteqv(x, end)

  /**
   * Return all the values in the Range as a List
   */
  def toList(implicit eA: Enum[A], oA: Order[A]): List[A] = {
    val lb = new ListBuilder[A]
   foreach{a => val _ = lb += a}
    lb.run
  }

  /**
    * Apply function f to each element in range [star, end]
    */
  def foreach(f: A => Unit)(implicit discrete: Enum[A], order: Order[A]): Unit = {
    val ignore = gen(start, end, List.empty)(f)
  }

  def map[B](f: A => B)(implicit discrete: Enum[A], order: Order[A]): List[B] =
    genMap[B](start, end, List.empty)(f)

  def foldLeft[B](s: B, f: (B, A) => B)(implicit discrete: Enum[A], order: Order[A]): B =
    generate.foldLeft(s)(f)

  private def genMap[B](x: A, y: A, xs: List[B])(f: A => B)(implicit discrete: Enum[A], order: Order[A]): List[B] = {
    @tailrec def traverse(a: A, b: A, xs: List[B])(f: A => B)(discrete: Enum[A], order: Order[A]): List[B] = {
      if (order.compare(a, b) == 0) {
        xs ::: Nel(f(a), List.empty)
      } else if (discrete.adj(a, b)) {
        xs ::: Nel(f(a), Nel(f(b), List.empty))
      } else {
        traverse(discrete.succ(a), b, xs ::: (Nel(f(a), List.empty)))(f)(discrete,order)
      }
    }

    if(order.lt(x,y))
      traverse(x, y, xs)(f)(discrete,order)
    else
      traverse(x, y, xs)(f)(new Enum[A] {
        override def pred(x: A): A = discrete.succ(x)
        override def succ(x: A): A = discrete.pred(x)
      }, order.reverse)
  }

  private def gen(x: A, y: A, xs: List[A])(f: A=>Unit)(implicit discrete: Enum[A], order: Order[A]): List[A] = {
  @tailrec def traverse(a: A, b: A, xs: List[A])(f: A => Unit)(discrete: Enum[A], order: Order[A]): List[A] = {
      if (order.compare(a, b) == 0) {
        f(a)
        xs ::: Nel(a, List.empty)
      } else if (discrete.adj(a, b)) {
        f(a)
        f(b)
        xs ::: Nel(a, Nel(b, List.empty))
      }
      else {
        f(a)
        traverse(discrete.succ(a), b, xs ::: (Nel(a, List.empty)))(f)(discrete,order)
      }
    }

    if(order.lt(x,y))
      traverse(x, y, xs)(f)(discrete,order)
    else
      traverse(x, y, xs)(f)(new Enum[A] {
        override def pred(x: A): A = discrete.succ(x)
        override def succ(x: A): A = discrete.pred(x)
      }, order.reverse)
  }

  private [dogs] def isEmpty: Boolean = false

  def apply(start: A, end: A): Range[A] = Range.apply(start, end)

  override def toString: String = if (isEmpty) s"[]" else s"[$start, $end]"

}

object Range {
  def apply[A](x: A, y: A) = new Range[A](x, y)

  def empty[A](): Range[A] = EmptyRange()

  private [dogs] case object EmptyRange extends Range[Option[Nothing]](None(), None()) {
    def apply[A]() = this.asInstanceOf[A]

    def unapply[A](r: Range[A]) = r.isEmpty

    override def isEmpty = true
  }
}
