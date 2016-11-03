package com.example

import java.util.Date

import akka.actor.Actor.Receive
import akka.actor._

case class DoctorVisitCompleted(
                               val patientId: String,
                               val firstName: String,
                               val date: Date,
                               val patientDetails: PatientDetails) {
  def this(patientId: String, firstName: String, date: Date) = {
    this(patientId, firstName, date, PatientDetails(null, null, null))
  }

  def carrier = patientDetails.carrier
  def lastName = patientDetails.lastName
  def socialSecurityNumber = patientDetails.socialSecurityNumber
}

case class PatientDetails(val lastName: String, val socialSecurityNumber: String, val carrier: String)

case class VisitCompleted(dispatcher: ActorRef)

object ContentEnricherDriver extends CompletableApp(3) {
  val accountingSystemDispatcher = system.actorOf(Props[AccountingSystemDispatcher], "accountingSystem")
  val accountingEnricherDispatcher = system.actorOf(Props(classOf[AccountingEnricherDispatcher], accountingEnricherDispatcher), "accountingDispatcher")
  val scheduledDoctorVisit = system.actorOf(Props(classOf[ScheduledDoctorVisit], "123456789", "John", "scheduleVisit"))
  scheduledDoctorVisit ! VisitCompleted(accountingEnricherDispatcher)

  awaitCompletion
  println("ContentEnricher: is completed.")
}

class AccountingEnricherDispatcher(val accountingSystemDispatcher: ActorRef) extends Actor {
  def receive = {
    case doctorVisitCompleted: DoctorVisitCompleted =>
      println("AccountingEnricherDispatcher: querying and forarding.")
      // query the enriching patient information...
      // ...
      val lastName = "Doe"
      val carrier = "Kaiser"
      val socialSecurityNumber = "111-22-3333"
      val enrichedDoctorVisitCompleted = DoctorVisitCompleted(
        doctorVisitCompleted.patientId,
        doctorVisitCompleted.firstName,
        doctorVisitCompleted.date,
        PatientDetails(lastName, socialSecurityNumber, carrier)
      )
      accountingSystemDispatcher forward enrichedDoctorVisitCompleted
      ContentEnricherDriver.completedStep()
    case _ =>
      println("AccountEnricherDispatcher: received unexpected message")
  }
}

class AccountingSystemDispatcher extends Actor {
  def receive = {
    case doctorVisitCompleted: DoctorVisitCompleted =>
      println("AccountingSystemDispatcher: sending to Accounting System...")
      ContentEnricherDriver.completedStep()
    case _ =>
      println("AccountingSystemDispatcher: received unexpected message")
  }
}

class ScheduledDoctorVisit(val patientId: String, val firstName: String) extends Actor {
  var completeOn: Date = _

  override def receive: Receive = {
    case visitCompleted: VisitCompleted =>
      println("ScheduledDoctorVisit: completing visit.")
      completeOn = new Date()
      visitCompleted.dispatcher ! new DoctorVisitCompleted(patientId, firstName, completeOn)
      ContentEnricherDriver.completedStep()
    case _ =>
      println("ScheduledDoctorVisit: received unexpected message")
  }
}