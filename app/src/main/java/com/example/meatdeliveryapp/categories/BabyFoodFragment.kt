package com.example.meatdeliveryapp.categories

import android.content.ContentValues
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.meatdeliveryapp.Product
import com.example.meatdeliveryapp.R
import com.example.meatdeliveryapp.SingletonCart
import com.example.meatdeliveryapp.databinding.FragmentBabyFoodBinding
import com.example.meatdeliveryapp.Data.ProductTest
import com.example.meatdeliveryapp.databinding.FragmentSnacksBinding
import com.example.meatdeliveryapp.recyclerProduct.AdapterProduct
import com.example.meatdeliveryapp.recyclerProduct.OnProductClickListener
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.util.concurrent.ExecutionException


class BabyFoodFragment : Fragment() , OnProductClickListener {
    private lateinit var binding: FragmentBabyFoodBinding
    private lateinit var adapters: Array<AdapterProduct>
    private lateinit var productRefs: Array<DatabaseReference>
    private lateinit var thread: Array<Thread>
    private lateinit var database: FirebaseDatabase
    private val handler = Handler()
    private lateinit var categoryRef: DatabaseReference
    private lateinit var threadCategory: Thread

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBabyFoodBinding.inflate(layoutInflater)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = FirebaseDatabase.getInstance()
        categoryRef = database.getReference("Categories")

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
        SingletonCart.loadProductList(requireContext())

        adapters = Array(2) { AdapterProduct(mutableListOf(), this) }
        productRefs = arrayOf(
            categoryRef.child("Baby").child("diapers"),
            categoryRef.child("Baby").child("nutrition")
        )


        thread = Array(2) { Thread() }


        thread[0] = Thread {
            val productList = loadData(productRefs[0])
            adapters[0] = AdapterProduct(productList, this)
            val layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL, false
            )
            handler.post {
                binding.recycler.adapter = adapters[0]
                binding.recycler.layoutManager = layoutManager

            }
        }
        thread[0].start()

        thread[1] = Thread {
            val productList2 = loadData(productRefs[1])
            adapters[1] = AdapterProduct(productList2, this)
            val layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL, false
            )
            handler.post {
                binding.recycler2.adapter = adapters[1]
                binding.recycler2.layoutManager = layoutManager

            }
        }
        thread[1].start()


    }
    private fun loadData(productRefs: DatabaseReference): MutableList<Product> {
        val productList = mutableListOf<Product>()

        try {
            val products = Tasks.await(productRefs.get())
            val p = ArrayList<Product>(products.getValue<Map<String, Product>>()!!.values)

            productList.addAll(p)
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return productList
    }

    override fun onPause() {
        super.onPause()
        SingletonCart.saveProductList(requireContext())

    }

    override fun onDestroy() {
        super.onDestroy()
        thread[0].interrupt()
        thread[1].interrupt()
    }

    override fun onResume() {
        SingletonCart.loadProductList(requireContext())
        super.onResume()
    }

    companion object {
        @JvmStatic
        fun newInstance() = BreadFragment()
    }

    override fun onAddProduct(product: Product) {
        SingletonCart.addProduct(
            Product(
                quantity = 1,
                name = product.name,
                id = product.id,
                price = product.price,
                imageUrl = product.imageUrl
            )
        )
        Toast.makeText(activity, "Добавлено в корзину", Toast.LENGTH_SHORT).show()
    }

    override fun onDeleteProduct(product: Product) {
        SingletonCart.deleteProduct(product)
        Toast.makeText(activity, "Удалено из корзины", Toast.LENGTH_SHORT).show()
    }
}