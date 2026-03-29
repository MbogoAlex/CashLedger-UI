package com.records.pesa.service.transaction.function;

import com.records.pesa.db.dao.CategoryDao;
import com.records.pesa.db.dao.TransactionsDao;
import com.records.pesa.db.models.CategoryKeyword;
import com.records.pesa.db.models.DeletedTransaction;
import com.records.pesa.db.models.Transaction;
import com.records.pesa.db.models.TransactionCategory;
import com.records.pesa.db.models.TransactionCategoryCrossRef;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SAFARICOM DATA MINIMIZATION UPDATE (March 24, 2026)
 * 
 * This class handles transaction categorization with support for masked phone numbers.
 * 
 * When comparing entities for category matching:
 * - "BENARD KOECH 0723378780" (old keyword) matches "BENARD KOECH 072***8780" (new transaction)
 * - "BENARD KOECH 072***8780" (new keyword) matches "BENARD KOECH 072***8780" (new transaction)
 * 
 * Both exact matching and intelligent fuzzy matching are used to ensure categories work
 * correctly across the phone number format change.
 */
public class TransactionInsertion {
    
    /**
     * Helper class to analyze whether a text segment represents a phone number
     */
    private static class PhoneAnalysis {
        boolean isPhone;
        double confidence;
        int length;
        int digits;
        int letters;
        int symbols;
        double digitRatio;
        
        PhoneAnalysis(String text) {
            if (text == null || text.trim().isEmpty()) {
                this.isPhone = false;
                this.confidence = 0.0;
                return;
            }
            
            text = text.trim();
            this.length = text.length();
            this.digits = countMatches(text, "\\d");
            this.letters = countMatches(text, "[a-zA-Z]");
            this.symbols = countMatches(text, "[^a-zA-Z0-9\\s]");
            this.digitRatio = this.length > 0 ? (double) this.digits / this.length : 0.0;
            
            // Phone number heuristics
            boolean lengthOk = this.length >= 7 && this.length <= 13;
            boolean hasDigits = this.digits >= 4;
            boolean digitRatioOk = this.digitRatio > 0.3;
            boolean notAllLetters = this.letters < this.length * 0.5;
            boolean hasMasking = this.symbols > 0 || countMatches(text, "\\s") > 1;
            
            // Calculate confidence score
            this.confidence = 0.0;
            if (lengthOk) this.confidence += 0.2;
            if (hasDigits) this.confidence += 0.3;
            if (digitRatioOk) this.confidence += 0.3;
            if (notAllLetters) this.confidence += 0.1;
            if (hasMasking) this.confidence += 0.1;
            
            // Is it a phone? Need at least 60% confidence
            this.isPhone = this.confidence >= 0.6;
        }
        
        private int countMatches(String text, String regex) {
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(text);
            int count = 0;
            while (m.find()) count++;
            return count;
        }
    }
    
    /**
     * Extract phone pattern preserving visible digits in positions
     * 
     * Examples:
     *   "0723378780" -> "0723378780" (all visible)
     *   "072***8780" -> "072___8780" (middle masked)
     *   "***3378780" -> "___3378780" (first masked)
     */
    private String extractPhonePattern(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "";
        }
        
        phone = phone.trim();
        
        StringBuilder pattern = new StringBuilder();
        int digitCount = 0;
        
        // Scan character by character
        for (int i = 0; i < phone.length() && digitCount < 10; i++) {
            char c = phone.charAt(i);
            if (Character.isDigit(c)) {
                pattern.append(c);
                digitCount++;
            } else if (!Character.isWhitespace(c) && !Character.isLetter(c)) {
                // Masking character (*, #, x, -, etc.)
                pattern.append('_');
                digitCount++;
            }
        }
        
        String builtPattern = pattern.toString();
        
        // If we have fewer than 10 positions, pad appropriately
        if (builtPattern.length() < 10 && builtPattern.length() >= 4) {
            String digitsOnly = phone.replaceAll("[^0-9]", "");
            boolean endsWithDigits = phone.length() > 0 && Character.isDigit(phone.charAt(phone.length() - 1));
            
            if (digitsOnly.length() == 10) {
                return digitsOnly;
            } else if (digitsOnly.length() >= 4) {
                StringBuilder flexPattern = new StringBuilder();
                int pos = 0;
                
                for (int i = 0; i < phone.length(); i++) {
                    char c = phone.charAt(i);
                    if (Character.isDigit(c)) {
                        flexPattern.append(c);
                        pos++;
                    } else if (!Character.isWhitespace(c)) {
                        flexPattern.append('_');
                        pos++;
                    }
                    if (pos >= 10) break;
                }
                
                // Pad to 10 if needed
                while (flexPattern.length() < 10) {
                    if (endsWithDigits) {
                        flexPattern.insert(0, '_');
                    } else {
                        flexPattern.append('_');
                    }
                }
                
                return flexPattern.substring(0, Math.min(10, flexPattern.length()));
            }
        }
        
        // For full 10-digit number
        String digitsOnly = phone.replaceAll("[^0-9]", "");
        if (digitsOnly.length() == 10) {
            return digitsOnly;
        }
        
        return builtPattern.isEmpty() ? digitsOnly : builtPattern;
    }
    
    /**
     * Normalize entity for consistent matching
     * 
     * Examples:
     *   "BENARD KOECH 0723378780" → "BENARD KOECH 0723378780"
     *   "BENARD KOECH 072***8780" → "BENARD KOECH 072___8780"
     */
    private String normalizeEntity(String entity) {
        if (entity == null || entity.trim().isEmpty()) {
            return entity;
        }
        
        String original = entity.trim();
        
        // Strategy 1: Full phone (10 consecutive digits at end)
        Pattern fullPhonePattern = Pattern.compile("\\s+(\\d{10})$");
        Matcher fullPhoneMatcher = fullPhonePattern.matcher(original);
        if (fullPhoneMatcher.find()) {
            String name = original.substring(0, fullPhoneMatcher.start()).trim();
            String phone = fullPhoneMatcher.group(1);
            String phonePattern = extractPhonePattern(phone);
            return name + " " + phonePattern;
        }
        
        // Strategy 2: Any trailing pattern that could be masked phone (13 down to 7 chars)
        for (int length = 13; length >= 7; length--) {
            String patternStr = "\\s+([\\S\\s]{" + length + "})$";
            Pattern p = Pattern.compile(patternStr);
            Matcher m = p.matcher(original);
            
            if (m.find()) {
                String potentialPhone = m.group(1).trim();
                PhoneAnalysis analysis = new PhoneAnalysis(potentialPhone);
                
                if (analysis.isPhone) {
                    String name = original.substring(0, m.start()).trim();
                    String phonePattern = extractPhonePattern(potentialPhone);
                    if (!phonePattern.isEmpty()) {
                        return name + " " + phonePattern;
                    }
                }
            }
        }
        
        // Strategy 3: Trailing sequence with digits and non-letters
        Pattern trailingPattern = Pattern.compile("\\s+([\\d\\W]{7,13})$");
        Matcher trailingMatcher = trailingPattern.matcher(original);
        if (trailingMatcher.find()) {
            String potentialPhone = trailingMatcher.group(1).trim();
            PhoneAnalysis analysis = new PhoneAnalysis(potentialPhone);
            
            if (analysis.isPhone) {
                String name = original.substring(0, trailingMatcher.start()).trim();
                String phonePattern = extractPhonePattern(potentialPhone);
                if (!phonePattern.isEmpty()) {
                    return name + " " + phonePattern;
                }
            }
        }
        
        // Strategy 4: Check last token
        String[] tokens = original.split("\\s+");
        if (tokens.length >= 2) {
            String lastToken = tokens[tokens.length - 1];
            PhoneAnalysis analysis = new PhoneAnalysis(lastToken);
            
            if (analysis.isPhone) {
                StringBuilder name = new StringBuilder();
                for (int i = 0; i < tokens.length - 1; i++) {
                    if (i > 0) name.append(" ");
                    name.append(tokens[i]);
                }
                String phonePattern = extractPhonePattern(lastToken);
                if (!phonePattern.isEmpty()) {
                    return name.toString() + " " + phonePattern;
                }
            }
        }
        
        return original;
    }
    
    /**
     * Smart entity comparison that matches old and new phone formats
     * 
     * Compares two normalized entities and returns true if they represent the same person.
     * 
     * Logic: All visible digits (non-underscore) must match in their positions
     * 
     * Examples:
     *   "BENARD KOECH 0723378780" vs "BENARD KOECH 072___8780" → TRUE (visible digits match)
     *   "BENARD KOECH 072___8780" vs "BENARD KOECH 072___8780" → TRUE (exact match)
     *   "BENARD KOECH 072___8780" vs "BENARD KOECH 071___8780" → FALSE (position 2 differs: 2 vs 1)
     *   "JOHN DOE 0723378780" vs "BENARD KOECH 0723378780" → FALSE (names differ)
     */
    private boolean entitiesMatch(String entity1, String entity2) {
        if (entity1 == null || entity2 == null) {
            return false;
        }
        
        // Exact match (fast path)
        if (entity1.equalsIgnoreCase(entity2)) {
            return true;
        }
        
        // Normalize both entities
        String normalized1 = normalizeEntity(entity1);
        String normalized2 = normalizeEntity(entity2);
        
        // After normalization, check exact match again
        if (normalized1.equalsIgnoreCase(normalized2)) {
            return true;
        }
        
        // Fuzzy match: compare character by character
        // All visible (non-underscore) characters must match in their positions
        int maxLen = Math.max(normalized1.length(), normalized2.length());
        
        for (int i = 0; i < maxLen; i++) {
            char c1 = i < normalized1.length() ? normalized1.charAt(i) : ' ';
            char c2 = i < normalized2.length() ? normalized2.charAt(i) : ' ';
            
            // Convert to lowercase for comparison
            c1 = Character.toLowerCase(c1);
            c2 = Character.toLowerCase(c2);
            
            // If one is underscore (masked) and other is digit/letter, skip
            if (c1 == '_' && c2 != '_') continue;
            if (c2 == '_' && c1 != '_') continue;
            
            // If both are real characters, they must match
            if (c1 != '_' && c2 != '_') {
                if (c1 != c2) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    public void addTransaction(Transaction transaction, TransactionsDao transactionsDao, List<TransactionCategory> categories, CategoryDao categoryDao) {

        List<DeletedTransaction> deletedTransactions = transactionsDao.getDeletedTransactionEntities();
        List<String> deletedItems = new ArrayList<>();

        if(!deletedTransactions.isEmpty()) {
            for(DeletedTransaction deletedTransaction : deletedTransactions) {
                deletedItems.add(deletedTransaction.getEntity().toLowerCase());
            }
        }

        if(!deletedItems.contains(transaction.getEntity().toLowerCase())) {
            long transactionId = transactionsDao.insertTransactionBlocking(transaction);

            // -1 means the transactionCode already exists (IGNORE conflict) — skip, do not re-map categories
            if (transactionId == -1L) return;

            Transaction transaction1 = transactionsDao.getStaticTransactionById((int) transactionId);

            if(!categories.isEmpty()) {
                for(TransactionCategory category : categories) {
                    addTransactionToCategory(transaction1, category, categoryDao);
                }
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
        
        // Step 1: Check for exact or fuzzy entity match using smart comparison
        CategoryKeyword matchingKeyword = null;
        for (CategoryKeyword categoryKeyword : categoryKeywords) {
            // Use smart entity matching that handles both old and new phone formats
            if (entitiesMatch(transaction.getEntity(), categoryKeyword.getKeyword())) {
                matchingKeyword = categoryKeyword;
                break;
            }
        }

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
        } else {
            // Step 2: Fall back to "contains" matching for category auto-assignment
            for (String keyword : category.getContains()) {
                if (!keyword.isEmpty()) {
                    if (transaction.getEntity().toLowerCase().contains(keyword.toLowerCase())) {
                        // Check if a matching keyword already exists
                        boolean keywordExists = false;
                        for (CategoryKeyword ck : categoryKeywords) {
                            if (entitiesMatch(transaction.getEntity(), ck.getKeyword())) {
                                keywordExists = true;
                                break;
                            }
                        }
                        
                        if (!keywordExists) {
                            try {
                                System.out.println("ADDING " + transaction.getEntity() + " KEYWORD");
                                CategoryKeyword categoryKeyword = new CategoryKeyword(0, "", "", 0);
                                categoryKeyword.setKeyword(transaction.getEntity());
                                categoryKeyword.setNickName(null);
                                categoryKeyword.setCategoryId(category.getId());
                                categoryDao.insertCategoryKeywordRunBlocking(categoryKeyword);

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
                    }
                }
            }
        }

    }
}



