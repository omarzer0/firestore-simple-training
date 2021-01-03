package omar.az.firestoreuploaddata

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.lang.StringBuilder

class MainActivity : AppCompatActivity() {

    private val personCollectionRef = Firebase.firestore.collection("persons")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSave.setOnClickListener {
            val person = getOldPersonData()
            savePersonData(person)
        }

        btnRetrieve.setOnClickListener {
            retrievePersons()
        }

        btnUpdate.setOnClickListener {
            val oldPerson = getOldPersonData()
            val newPerson = getNewPersonMap()
            updatePersonData(oldPerson, newPerson)
        }

        btnDelete.setOnClickListener {
            deletePersonData(getOldPersonData())
        }
        subscribeTpRealTimeUpdate()
    }

    private fun getOldPersonData(): Person {
        val firstName = etFName.text.toString()
        val lastName = etLName.text.toString()
        val age = etAge.text.toString().toInt()
        return Person(firstName, lastName, age)
    }

    private fun getNewPersonMap(): Map<String, Any> {
        val firstName = etNewFName.text.toString()
        val lastName = etNewLName.text.toString()
        val age = etNewAge.text.toString()
        val map = mutableMapOf<String, Any>()
        if (firstName.isNotEmpty()) {
            map["firstName"] = firstName
        }
        if (lastName.isNotEmpty()) {
            map["lastName"] = lastName
        }

        if (age.isNotEmpty()) {
            map["age"] = age.toInt()
        }

        return map
    }

    private fun updatePersonData(person: Person, newPersonMap: Map<String, Any>) =
        CoroutineScope(Dispatchers.IO).launch {
            val personQuery = personCollectionRef
                .whereEqualTo("firstName", person.firstName)
                .whereEqualTo("lastName", person.lastName)
                .whereEqualTo("age", person.age)
                .get().await()

            // after searching the doc for that person if it is empty then no person with these fields exists
            if (personQuery.documents.isNotEmpty()) {
                for (document in personQuery) {
                    try {
                        personCollectionRef.document(document.id)
                            .set(newPersonMap, SetOptions.merge()).await()
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "no result found", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun deletePersonData(person: Person) = CoroutineScope(Dispatchers.IO).launch {
        val personQuery = personCollectionRef
            .whereEqualTo("firstName", person.firstName)
            .whereEqualTo("lastName", person.lastName)
            .whereEqualTo("age", person.age)
            .get().await()

        // after searching the doc for that person if it is empty then no person with these fields exists
        if (personQuery.documents.isNotEmpty()) {
            for (document in personQuery) {
                try {
                    personCollectionRef.document(document.id).delete().await()
//                    personCollectionRef.document(document.id).update(mapOf(
//                        "firstName" to FieldValue.delete()
//                    ))

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "no result found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun savePersonData(person: Person) = CoroutineScope(Dispatchers.IO).launch {
        try {
            personCollectionRef.add(person).await()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "uploaded successfully", Toast.LENGTH_SHORT)
                    .show()

            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()

            }
        }
    }

    private fun retrievePersons() = CoroutineScope(Dispatchers.IO).launch {
        val fromAge = etFrom.text.toString().toInt()
        val toAge = etTo.text.toString().toInt()
        try {
            val querySpanShot = personCollectionRef
                .whereGreaterThan("age", fromAge)
                .whereLessThan("age", toAge)
                .orderBy("age").get().await()

            val sb = getAppendedText(querySpanShot)
            withContext(Dispatchers.Main) {
                tvRetrieve.text = sb.toString()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getAppendedText(querySpanShot: QuerySnapshot): StringBuilder {
        val sb = StringBuilder()
        for (document in querySpanShot.documents) {
            //                val person = document.toObject(Person::class.java) java approach
            val person = document.toObject<Person>()
            sb.append("$person\n\n\n")
        }
        return sb
    }

    private fun subscribeTpRealTimeUpdate() {
        // addSnapshotListener retrieve a real time changes of the document
        personCollectionRef.addSnapshotListener { querySnapShot, error ->
            // if the error is not null (error occurs)
            error?.let {
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                // return to not complete executing the code below
                // @addSnapshotListener to exit the listener
                return@addSnapshotListener
            }
            // now we have no error but we check if the querySnapShot is not null
            querySnapShot?.let {
                val sb = getAppendedText(querySnapShot)
                tvRetrieve.text = sb.toString()
            }
        }
    }

}