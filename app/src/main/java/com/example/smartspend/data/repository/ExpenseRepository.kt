package com.example.smartspend.data.repository

import com.example.smartspend.data.local.Expense
import com.example.smartspend.data.local.ExpenseDao
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    
    val totalSpending: Flow<Double?> = expenseDao.getTotalSpending()
    
    suspend fun insert(expense: Expense): Long {
        return expenseDao.insertExpense(expense)
    }
    
    suspend fun delete(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }
    
    suspend fun getById(id: Long): Expense? {
        return expenseDao.getExpenseById(id)
    }
    
    fun getSpendingByCategory(category: String): Flow<Double?> {
        return expenseDao.getSpendingByCategory(category)
    }

    fun getExpensesByDateRange(startDate: String, endDate: String): Flow<List<Expense>> {
        return expenseDao.getExpensesByDateRange(startDate, endDate)
    }

    fun getTotalSpendingByDateRange(startDate: String, endDate: String): Flow<Double?> {
        return expenseDao.getTotalSpendingByDateRange(startDate, endDate)
    }
}
