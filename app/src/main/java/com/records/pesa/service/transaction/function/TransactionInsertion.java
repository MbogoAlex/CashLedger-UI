package com.records.pesa.service.transaction.function;

import com.records.pesa.db.dao.CategoryDao;
import com.records.pesa.db.dao.TransactionsDao;
import com.records.pesa.db.models.CategoryKeyword;
import com.records.pesa.db.models.Transaction;
import com.records.pesa.db.models.TransactionCategory;
import com.records.pesa.db.models.TransactionCategoryCrossRef;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionInsertion {
    public void addTransaction(Transaction transaction, TransactionsDao transactionsDao, List<TransactionCategory> categories, CategoryDao categoryDao) {
        long transactionId = transactionsDao.insertTransactionBlocking(transaction);

        Transaction transaction1 = transactionsDao.getStaticTransactionById((int) transactionId);

        if(!categories.isEmpty()) {
            for(TransactionCategory category : categories) {
                addTransactionToCategory(transaction1, category, categoryDao);
            }
        }
    }

    void addTransactionToCategory(Transaction transaction, TransactionCategory category, CategoryDao categoryDao) {
//        System.out.println("ADDING_TRANSACTION: "+ transaction.toString());
        Double updatedTimes = category.getUpdatedTimes();

        if(updatedTimes == null) {
            updatedTimes = 0.0;
        }


        List<CategoryKeyword> categoryKeywords = categoryDao.getStaticCategoryKeywords(category.getId());
        // Step 1: Pre-process the categoryKeywords into a HashMap for faster lookup.
        Map<String, CategoryKeyword> keywordMap = new HashMap<>();
        for (CategoryKeyword categoryKeyword : categoryKeywords) {
            keywordMap.put(categoryKeyword.getKeyword().toLowerCase(), categoryKeyword);
        }

// Step 2: Look up the transaction entity in the keyword map.
        String entityLower = transaction.getEntity().toLowerCase();
        CategoryKeyword matchingKeyword = keywordMap.get(entityLower);

        if (matchingKeyword != null) {

            try {
                category.setUpdatedTimes(updatedTimes + 1.0);
                category.setUpdatedAt(LocalDateTime.now());
                categoryDao.updateCategoryRunBlocking(category);
                TransactionCategoryCrossRef transactionCategoryCrossRef = new TransactionCategoryCrossRef();
                transactionCategoryCrossRef.setTransactionId(transaction.getId());
                transactionCategoryCrossRef.setCategoryId(category.getId());
                categoryDao.insertCategoryTransactionMappingRunBlocking(transactionCategoryCrossRef);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(!category.getContains().isEmpty()) {
            for(String contains : category.getContains()) {
                if(transaction.getEntity().toLowerCase().contains(contains.toLowerCase())) {
                    if(!categoryKeywords.contains(transaction.getEntity())) {
                        CategoryKeyword categoryKeyword = new CategoryKeyword(0, "", "", 0);
                        categoryKeyword.setKeyword(transaction.getEntity());
                        categoryKeyword.setNickName(null);
                        categoryKeyword.setCategoryId(category.getId());
                        categoryDao.insertCategoryKeywordRunBlocking(categoryKeyword);
                        TransactionCategoryCrossRef transactionCategoryCrossRef = new TransactionCategoryCrossRef();
                        transactionCategoryCrossRef.setTransactionId(transaction.getId());
                        transactionCategoryCrossRef.setCategoryId(category.getId());
                        categoryDao.insertCategoryTransactionMappingRunBlocking(transactionCategoryCrossRef);
                    }

                }
            }
        }
    }
}



