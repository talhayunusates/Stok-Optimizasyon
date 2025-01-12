package com.example.stokoptimizasyon


import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.apache.commons.math3.optim.linear.LinearConstraint
import org.apache.commons.math3.optim.linear.LinearConstraintSet
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction
import org.apache.commons.math3.optim.linear.NonNegativeConstraint
import org.apache.commons.math3.optim.linear.Relationship
import org.apache.commons.math3.optim.linear.SimplexSolver
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val costPerStorage = findViewById<EditText>(R.id.cost_per_storage)
        val costPerOrder = findViewById<EditText>(R.id.cost_per_order)
        val weeklyDemand = findViewById<EditText>(R.id.weekly_demand)
        val maxCapacity = findViewById<EditText>(R.id.max_capacity)
        val userStockInput = findViewById<EditText>(R.id.user_stock)
        val userOrderInput = findViewById<EditText>(R.id.user_order)
        val profitPerUnitInput = findViewById<EditText>(R.id.profit_per_unit)

        val calculateButton = findViewById<Button>(R.id.calculate_button)
        val resultText = findViewById<TextView>(R.id.result_text)

        calculateButton.setOnClickListener {
            try {
                // Kullanıcı girişlerini al
                val storageCost = costPerStorage.text.toString().toDouble()
                val orderCost = costPerOrder.text.toString().toDouble()
                val demand = weeklyDemand.text.toString().toDouble()
                val capacity = maxCapacity.text.toString().toDouble()
                val userStock = userStockInput.text.toString().toDouble()
                val userOrder = userOrderInput.text.toString().toDouble()
                val profitPerUnit = profitPerUnitInput.text.toString().toDouble()

                // Optimize et ve sonucu al
                val optimalSolution = optimize(storageCost, orderCost, demand, capacity, userStock, userOrder, profitPerUnit)

                // Sonucu ekrana yaz
                resultText.text = optimalSolution
            } catch (e: Exception) {
                resultText.text = "Lütfen tüm alanları doğru doldurduğunuzdan emin olun!"
            }
        }
    }

    private fun optimize(
        storageCost: Double,
        orderCost: Double,
        demand: Double,
        capacity: Double,
        userStock: Double,
        userOrder: Double,
        profitPerUnit: Double
    ): String {
        // Amaç fonksiyonu ve kısıtlamalar
        val objectiveFunction = LinearObjectiveFunction(doubleArrayOf(storageCost, orderCost), 0.0)

        val constraints = ArrayList<LinearConstraint>()
        constraints.add(LinearConstraint(doubleArrayOf(-1.0, -1.0), Relationship.LEQ, -demand)) // stok + sipariş >= talep
        constraints.add(LinearConstraint(doubleArrayOf(1.0, 0.0), Relationship.LEQ, capacity)) // stok <= kapasite

        val problem = LinearConstraintSet(constraints)
        val solver = SimplexSolver()
        val solution = solver.optimize(objectiveFunction, problem, GoalType.MINIMIZE, NonNegativeConstraint(true))

        val optimalStock = solution.point[0]
        val optimalOrder = solution.point[1]
        val optimalTotalCost = solution.value

        // Kullanıcı çözümü maliyeti
        val userTotalCost = userStock * storageCost + userOrder * orderCost

        // Haftalık kazançlar
        val optimalWeeklyProfit = (optimalStock + optimalOrder) * profitPerUnit - optimalTotalCost
        val userWeeklyProfit = (userStock + userOrder) * profitPerUnit - userTotalCost

        // Aylık ve yıllık kazançlar
        val optimalMonthlyProfit = optimalWeeklyProfit * 4
        val optimalYearlyProfit = optimalWeeklyProfit * 52
        val userMonthlyProfit = userWeeklyProfit * 4
        val userYearlyProfit = userWeeklyProfit * 52

        // Kazanç farkı yüzdesi
        val profitDifferencePercentage = if (optimalWeeklyProfit > 0) {
            (Math.abs(optimalWeeklyProfit - userWeeklyProfit) / optimalWeeklyProfit) * 100
        } else {
            0.0
        }

        // Optimal çözüm önerisi
        val suggestion = if (demand > capacity) {
            "Talep depo kapasitesinden büyük! Optimal çözüm için şunları yapabilirsiniz:\n" +
                    "- Depo kapasitesini en az %.2f birim artırın.\n".format(demand - capacity) +
                    "- Sipariş miktarını değiştirerek talebi karşılayın.\n"
        } else {
            "Optimal çözüme ulaşmak için depo miktarınızı %.2f ve sipariş miktarınızı %.2f olarak ayarlayın.".format(
                optimalStock,
                optimalOrder
            )
        }

        return "Optimal Çözüm:\n" +
                "Depo miktarı: %.2f birim\n".format(optimalStock) +
                "Sipariş miktarı: %.2f birim\n".format(optimalOrder) +
                "Toplam maliyet: %.2f TL\n".format(optimalTotalCost) +
                "Haftalık kazanç: %.2f TL\n".format(optimalWeeklyProfit) +
                "Aylık kazanç: %.2f TL\n".format(optimalMonthlyProfit) +
                "Yıllık kazanç: %.2f TL\n".format(optimalYearlyProfit) +
                "\nKullanıcı Çözümü:\n" +
                "Depo miktarı: %.2f birim\n".format(userStock) +
                "Sipariş miktarı: %.2f birim\n".format(userOrder) +
                "Toplam maliyet: %.2f TL\n".format(userTotalCost) +
                "Haftalık kazanç: %.2f TL\n".format(userWeeklyProfit) +
                "Aylık kazanç: %.2f TL\n".format(userMonthlyProfit) +
                "Yıllık kazanç: %.2f TL\n".format(userYearlyProfit) +
                "\nOptimal kazanca uzaklık: %.2f%%\n".format(profitDifferencePercentage) +
                "\nTavsiye:\n$suggestion"
    }
}
