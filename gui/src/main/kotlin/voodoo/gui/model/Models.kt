//package moe.nikky.voodoo.model
//
//import javafx.beans.property.SimpleBooleanProperty
//import javafx.beans.property.SimpleIntegerProperty
//import javafx.beans.property.SimpleListProperty
//import javafx.beans.property.SimpleStringProperty
//import javafx.collections.ObservableList
//import tornadofx.*
//
///**
// * Created by nikky on 20/12/17.
// * @author Nikky
// * @version 1.0
// */
//
//data class Group(val name: String, val selected: Boolean = true, val children: List<Group>? = null)
//
//sealed class PersonTreeItem(open val name: String)
//object TreeRoot : PersonTreeItem("Departments")
//data class Department(override val name: String): PersonTreeItem(name)
//
//
//val group = Group("Parent", true,
//        listOf(
//                Group("Child 1"),
//                Group("Child 2"),
//                Group("Child 3", true, listOf(
//                        Group("Grand child 3.1", true,
//                                listOf(
//                                        Group("Great grandchild 3.1.1"),
//                                        Group("Great grandchild 3.1.2"))))
//                ),
//                Group("Child 4"))
//)
//
//class Person(name: String,
//             department: String,
//             email: String,
//             employees: List<Person> = emptyList()) {
//    val nameProperty = SimpleStringProperty(name)
//    var name by nameProperty
//
//    val departmentProperty = SimpleStringProperty(department)
//    var department by departmentProperty
//
//    val emailProperty = SimpleStringProperty(email)
//    var email by emailProperty
//
//    val employeesProperty = SimpleListProperty(employees.observable())
//    var employees by employeesProperty
//}
//
//val persons = listOf(
//        Person("Mary Hanes", "IT Administration", "mary.hanes@contoso.com", listOf(
//                Person("Jacob Mays", "IT Help Desk", "jacob.mays@contoso.com"),
//                Person("John Ramsy", "IT Help Desk", "john.ramsy@contoso.com"))),
//        Person("Erin James", "Human Resources", "erin.james@contoso.com", listOf(
//                Person("Erlick Foyes", "Customer Service", "erlick.foyes@contoso.com"),
//                Person("Steve Folley", "Customer Service", "steve.folley@contoso.com"),
//                Person("Larry Cable", "Customer Service", "larry.cable@contoso.com")))
//).observable()
//
//class Region(selected: Boolean, id: Int, name: String, country: String, var branches: ObservableList<Branch>) {
//    val selectedProperty = SimpleBooleanProperty(selected)
//    var selected by selectedProperty
//
//    val idProperty = SimpleIntegerProperty(id)
//    var id by idProperty
//
//    val nameProperty = SimpleStringProperty(name)
//    var name by nameProperty
//
//    val countryProperty = SimpleStringProperty(country)
//    var country by countryProperty
//
//}
//
//data class Branch(val id: Int, var facilityCode: String, var city: String, var stateProvince: String)
//
//val regions = listOf(
//        Region(true,1,"Pacific Northwest", "USA",listOf(
//                Branch(1,"D","Seattle","WA"),
//                Branch(2,"W","Portland","OR")
//        ).observable()),
//        Region(true,2,"Alberta", "Canada",listOf(
//                Branch(3,"W","Calgary","AB")
//        ).observable()),
//        Region(false,3,"Midwest", "USA", listOf(
//                Branch(4,"D","Chicago","IL"),
//                Branch(5,"D","Frankfort","KY"),
//                Branch(6, "W","Indianapolis", "IN")
//        ).observable())
//).observable()
//
