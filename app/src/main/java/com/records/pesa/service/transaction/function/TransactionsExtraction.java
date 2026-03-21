package com.records.pesa.service.transaction.function;


import com.records.pesa.db.dao.CategoryDao;
import com.records.pesa.db.dao.TransactionsDao;
import com.records.pesa.db.models.Transaction;
import com.records.pesa.db.models.TransactionCategory;
import com.records.pesa.db.models.UserAccount;
import com.records.pesa.models.MessageData;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TransactionsExtraction {
    
    /**
     * SAFARICOM DATA MINIMIZATION UPDATE (March 24, 2026)
     * 
     * Phone number normalization helper methods to handle masked phone numbers.
     * 
     * Strategy: Create position-aware pattern preserving ALL visible digits
     * - NO assumptions about which positions are masked
     * - NO assumptions about how many positions are masked
     * - NO assumptions about what character is used for masking
     * 
     * Examples (all representing same phone number):
     *   "0723378780" → "0723378780" (full - all visible)
     *   "072***8780" → "072___8780" (middle masked)
     *   "***3378780" → "___3378780" (first 3 masked)
     *   "0723378***" → "0723378___" (last 3 masked)
     *   "07*3*7*780" → "07_3_7_780" (random positions)
     *   "072xxx8780" → "072___8780" (different mask char)
     * 
     * Ensures:
     * ✅ Preserves visible digits in correct positions
     * ✅ Different people with same name = DIFFERENT entities
     * ✅ Same person with different mask formats = SAME entity (if overlap exists)
     * 
     * NOTE: Complete matching requires visible digits to overlap. Example:
     *   "072___8780" vs "___3378780" → Cannot definitively match without full phone
     *   "072___8780" vs "0723378780" → Can match (second contains first's visible digits)
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
     * Extract flexible phone pattern preserving ALL visible digits with positions
     * 
     * NO ASSUMPTIONS about which digits are masked!
     * 
     * Examples (all same phone):
     *   "0723378780" -> "0723378780" (all visible)
     *   "072***8780" -> "072___8780" (middle masked)
     *   "***3378780" -> "___3378780" (first masked)
     *   "0723378***" -> "0723378___" (last masked)
     *   "07*3**8*80" -> "07_3__8_80" (random positions)
     * 
     * Strategy: Build position-aware pattern string
     */
    private String extractPhonePattern(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "";
        }
        
        phone = phone.trim();
        
        // Build a 10-character pattern string preserving visible digits
        StringBuilder pattern = new StringBuilder();
        int digitCount = 0;
        
        // Scan through the string and build pattern
        for (int i = 0; i < phone.length() && digitCount < 10; i++) {
            char c = phone.charAt(i);
            if (Character.isDigit(c)) {
                pattern.append(c);
                digitCount++;
            } else if (!Character.isWhitespace(c) && !Character.isLetter(c)) {
                // It's a masking character (*, #, -, x, etc.)
                pattern.append('_');
                digitCount++;
            }
        }
        
        // If we have fewer than 10 positions, we need to infer the rest
        // Look at what we have and try to build a 10-char pattern
        String builtPattern = pattern.toString();
        
        // If pattern is less than 10 chars, pad appropriately
        if (builtPattern.length() < 10 && builtPattern.length() >= 4) {
            // We have some visible digits - preserve them
            String digitsOnly = phone.replaceAll("[^0-9]", "");
            
            // Count continuous digit sequences to determine structure
            boolean startsWithDigits = phone.length() > 0 && Character.isDigit(phone.charAt(0));
            boolean endsWithDigits = phone.length() > 0 && Character.isDigit(phone.charAt(phone.length() - 1));
            
            if (digitsOnly.length() == 10) {
                // Full phone - return as-is
                return digitsOnly;
            } else if (digitsOnly.length() >= 4) {
                // Partial phone - build pattern showing what we know
                StringBuilder flexPattern = new StringBuilder();
                int pos = 0;
                
                // Iterate through original string and build position-aware pattern
                for (int i = 0; i < phone.length(); i++) {
                    char c = phone.charAt(i);
                    if (Character.isDigit(c)) {
                        flexPattern.append(c);
                        pos++;
                    } else if (!Character.isWhitespace(c)) {
                        // Masking character
                        flexPattern.append('_');
                        pos++;
                    }
                    if (pos >= 10) break;
                }
                
                // Pad to 10 if needed
                while (flexPattern.length() < 10) {
                    if (endsWithDigits) {
                        // Masking likely at front, pad front
                        flexPattern.insert(0, '_');
                    } else {
                        // Masking likely at end, pad end
                        flexPattern.append('_');
                    }
                }
                
                return flexPattern.substring(0, Math.min(10, flexPattern.length()));
            }
        }
        
        // For full 10-digit number, return as-is
        String digitsOnly = phone.replaceAll("[^0-9]", "");
        if (digitsOnly.length() == 10) {
            return digitsOnly;
        }
        
        // Return what we built
        return builtPattern.isEmpty() ? digitsOnly : builtPattern;
    }
    
    /**
     * Normalize entity for consistent matching across phone format changes
     * 
     * Strategy: Keep name + phone pattern with visible digits preserved
     * 
     * Examples:
     *   "BENARD KOECH 0723378780" → "BENARD KOECH 0723378780" (full)
     *   "BENARD KOECH 072***8780" → "BENARD KOECH 072___8780" (middle masked)
     *   "BENARD KOECH ***3378780" → "BENARD KOECH ___3378780" (first masked)
     *   "BENARD KOECH 0723378***" → "BENARD KOECH 0723378___" (last masked)
     * 
     * Important: Different people with same name are kept separate:
     *   "BENARD KOECH 0723378780" → "BENARD KOECH 0723378780"
     *   "BENARD KOECH 0712345678" → "BENARD KOECH 0712345678" (different person!)
     * 
     * Limitation: If first transaction has middle-masked format and second has
     * end-masked format, they will be stored as separate entities initially.
     * This is by design - we cannot definitively match without overlapping visible digits.
     * 
     * Mitigation: Most users will see transactions from same person in same format,
     * so this edge case should be rare. A future migration can consolidate if needed.
     */
    private String normalizeEntity(String entity) {
        if (entity == null || entity.trim().isEmpty()) {
            return entity;
        }
        
        String original = entity.trim();
        
        // Strategy 1: Try to find full phone (10 consecutive digits at end)
        Pattern fullPhonePattern = Pattern.compile("\\s+(\\d{10})$");
        Matcher fullPhoneMatcher = fullPhonePattern.matcher(original);
        if (fullPhoneMatcher.find()) {
            String name = original.substring(0, fullPhoneMatcher.start()).trim();
            String phone = fullPhoneMatcher.group(1);
            String phonePattern = extractPhonePattern(phone);
            return name + " " + phonePattern;
        }
        
        // Strategy 2: Look for ANY trailing pattern that could be a masked phone
        // Try different lengths from 13 down to 7 characters
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
        
        // Strategy 3: Look for trailing sequence with mix of digits and non-letters
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
        
        // No phone detected - return as-is
        return original;
    }
    
    /**
     * Detect if recipient text contains a phone-like pattern
     * Used to distinguish "Send Money" from "Pochi la Biashara"
     */
    private boolean hasPhoneLikePattern(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // Try full phone
        if (Pattern.compile("\\d{10}$").matcher(text).find()) {
            return true;
        }
        
        // Try to detect ANY phone pattern at end
        for (int length = 13; length >= 7; length--) {
            String patternStr = "[\\S\\s]{" + length + "}$";
            Pattern p = Pattern.compile(patternStr);
            Matcher m = p.matcher(text);
            
            if (m.find()) {
                String segment = m.group(0).trim();
                PhoneAnalysis analysis = new PhoneAnalysis(segment);
                if (analysis.isPhone && analysis.confidence >= 0.6) {
                    return true;
                }
            }
        }
        
        // Check last token
        String[] tokens = text.split("\\s+");
        if (tokens.length > 0) {
            String lastToken = tokens[tokens.length - 1];
            PhoneAnalysis analysis = new PhoneAnalysis(lastToken);
            if (analysis.isPhone && analysis.confidence >= 0.6) {
                return true;
            }
        }
        
        return false;
    }
    
    public Transaction extractTransactionDetails(MessageData messageDto, UserAccount userAccount, TransactionsDao transactionsDao, List<TransactionCategory> categories, CategoryDao categoryDao) {
        System.out.println("EXTRACTING TRANSACTION");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Transaction transactionDto = new Transaction(0, "", "", 0.0, 0.0, LocalDate.now(), LocalTime.now(), "", "", null, null, 0.0, "", 0);
        String message = messageDto.getBody();
        String realDate = String.valueOf(messageDto.getDate());
        String realTime = String.valueOf(messageDto.getTime());

        Matcher transactionCodeMatcher = Pattern.compile("\\b\\w{10}\\b").matcher(message);
        if (transactionCodeMatcher.find()) {
            String transactionCode = transactionCodeMatcher.group();
            String transactionType = null;
            Double transactionAmount = null;
            Double transactionCost = null;
            String transactionDate = null;
            String transactionTime = null;
            String sender = null;
            String recipient = null;
            Double balance = null;

            if (message.contains("sent to")) {

                // Parsing transaction amount
                Pattern amountPattern = Pattern.compile("Ksh([\\d ,]+\\.\\d{2})+ sent");
                Matcher amountMatcher = amountPattern.matcher(message);
                amountMatcher.find();
                transactionAmount = -1 * Double.parseDouble(amountMatcher.group(1).replace(",", "").replace(" ", ""));

                // Parsing transaction cost - UPDATED: comma optional (for M-PESA Ratiba format)
                Pattern costPattern = Pattern.compile("Transaction cost,?\\s*Ksh\\.?\\s*([\\d,]+(?:\\.\\d{2})?)");
                Matcher costMatcher = costPattern.matcher(message);
                costMatcher.find();
                transactionCost = -1 * Double.parseDouble(costMatcher.group(1).replace(",", ""));

                // Parsing transaction date and time
//                Pattern dateTimePattern = Pattern.compile("on (\\d{1,2}/\\d{1,2}/\\d{2}) at (\\d{1,2}:\\d{2} [AP]M)");
//                Matcher dateTimeMatcher = dateTimePattern.matcher(message);
//                dateTimeMatcher.find();
                transactionDate = realDate;
                transactionTime = realTime;

                // Parsing recipient
                Pattern recipientPattern = Pattern.compile("sent to (.+?)(?: on )");
                Matcher recipientMatcher = recipientPattern.matcher(message);
                recipientMatcher.find();
                recipient = recipientMatcher.group(1).replace("\u00A0", " ");  // Unicode for non-breaking space

                String str = recipient;

                String regex = "^(?:(?!Safaricom Offers).)+ for account (?!SAFARICOM DATA BUNDLES|Tunukiwa|TUNUKIWA|Talkmore)[a-zA-Z0-9 ]+";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(str);

                // UPDATED: Robust phone detection for data minimization (March 24, 2026)
                // Handles masked phone numbers: 072***8780, 072xxx8780, ***3378780, etc.
                if (hasPhoneLikePattern(str)) {
                    // If a phone-like pattern is found, classify it as "Send money"
                    transactionType = "Send Money";
                } else {
                    // If no phone pattern is found, classify it as "Pochi la Biashara"
                    transactionType = "Pochi la Biashara";
                }

                if (str.contains("SAFARICOM DATA BUNDLES") ||
                        str.contains("Tunukiwa") ||
                        str.contains("TUNUKIWA") ||
                        str.contains("Talkmore")) {

                    transactionType = "Airtime & Bundles";
                } else if (matcher.find()) {
                    // If no match for "Airtime & bundles", check for "Pay Bill"
                    transactionType = "Pay Bill";
                }

                if(recipient.contains("Safaricom")) {
                    transactionType = "Airtime & Bundles";
                }

                // Parsing balance
                Pattern balancePattern = Pattern.compile("New M-PESA balance is Ksh([\\d ,]+\\.\\d{2})");
                Matcher balanceMatcher = balancePattern.matcher(message);
                balanceMatcher.find();
                balance = Double.parseDouble(balanceMatcher.group(1).replace(",", "").replace(" ", ""));

                // Setting sender to "You"
                sender = "You";
            } else if (message.contains("Withdraw Ksh")) {
                transactionType = "Withdraw Cash";

                // Extract transaction amount
                Matcher amountMatcher = Pattern.compile("Withdraw Ksh([\\d ,]+\\.\\d{2})").matcher(message);
                amountMatcher.find();
                transactionAmount = -1 * Double.parseDouble(amountMatcher.group(1).replace(",", "").replace(" ", ""));

                // Extract transaction cost
                Matcher costMatcher = Pattern.compile("Transaction cost, ?Ksh\\.?\\s*([\\d,]+(?:\\.\\d{2})?)").matcher(message);
                costMatcher.find();
                transactionCost = -1 * Double.parseDouble(costMatcher.group(1).replace(",", "").replace(" ", ""));

                // Extract transaction date and time
//                Matcher dateTimeMatcher = Pattern.compile("on (\\d{1,2}/\\d{1,2}/\\d{2}) at (\\d{1,2}:\\d{2} [AP]M)").matcher(message);
//                dateTimeMatcher.find();
                transactionDate = realDate;
                transactionTime = realTime;

                // Extract recipient
                Matcher recipientMatcher = Pattern.compile("from\\s\\d+\\s-\\s[^.]+(?=\\sNew)").matcher(message);

                if (recipientMatcher.find()) {
                    recipient = recipientMatcher.group(0).trim();
                    recipient = "Withdrawal " + recipient;
                } else {
                    recipient = "N/A- Withdrawal from Mpesa-Shop";
                }

                sender = "You";

                // Extract balance
                Matcher balanceMatcher = Pattern.compile("New M-PESA balance is Ksh([\\d ,]+\\.\\d{2})").matcher(message);
                balanceMatcher.find();
                balance = Double.parseDouble(balanceMatcher.group(1).replace(",", "").replace(" ", ""));
            } else if (message.contains("paid to")) {
                transactionType = "Buy Goods and Services (till)";

                // Extract transaction amount
                Matcher amountMatcher = Pattern.compile("Ksh([\\d ,]+\\.\\d{2})+ paid to").matcher(message);
                amountMatcher.find();
                transactionAmount = -1 * Double.parseDouble(amountMatcher.group(1).replace(",", "").replace(" ", ""));

                // Extract transaction cost
                Matcher costMatcher = Pattern.compile("Transaction cost, ?Ksh\\.?\\s*([\\d,]+(?:\\.\\d{2})?)").matcher(message);
                costMatcher.find();
                transactionCost = -1 * Double.parseDouble(costMatcher.group(1).replace(",", "").replace(" ", ""));

                // Extract transaction date and time
//                Matcher dateTimeMatcher = Pattern.compile("on (\\d{1,2}/\\d{1,2}/\\d{2}) at (\\d{1,2}:\\d{2} [AP]M)").matcher(message);
//                dateTimeMatcher.find();
                transactionDate = realDate;
                transactionTime = realTime;

                sender = "You";

                // Extract recipient
                Matcher recipientMatcher = Pattern.compile("paid to (.+?(?=\\.))").matcher(message);
                recipientMatcher.find();
                recipient = recipientMatcher.group(1);

                // Extract balance
                Matcher balanceMatcher = Pattern.compile("New M-PESA balance is Ksh([\\d ,]+\\.\\d{2})").matcher(message);
                balanceMatcher.find();
                balance = Double.parseDouble(balanceMatcher.group(1).replace(",", "").replace(" ", ""));
            } else if(message.contains("to Hustler Fund")) {
                transactionType = "Hustler Fund";

                // Extract transaction amount
                Matcher amountMatcher = Pattern.compile("You have sent Ksh([\\d ,]+\\.\\d{2})+ to").matcher(message);
                amountMatcher.find();
                transactionAmount = -1 * Double.parseDouble(amountMatcher.group(1).replace(",", "").replace(" ", ""));

                transactionCost = 0.0;

                // Extract transaction date and time
//                Matcher dateTimeMatcher = Pattern.compile("on (\\d{1,2}/\\d{1,2}/\\d{2,4})\\s*at\\s*(\\d{1,2}:\\d{2} [AP]M)").matcher(message);
//                dateTimeMatcher.find();
                transactionDate = realDate;
                transactionTime = realTime;

                sender = "You";

                // Extract recipient
                Matcher recipientMatcher = Pattern.compile("to (.+?) on").matcher(message);
                recipientMatcher.find();
                recipient = recipientMatcher.group(1);

                // Extract balance
                Matcher balanceMatcher = Pattern.compile("New\\s*MPESA\\s*balance\\s*is\\s*Ksh([\\d ,]+\\.\\d{2})").matcher(message);
                balanceMatcher.find();
                balance = Double.parseDouble(balanceMatcher.group(1).replace(",", "").replace(" ", ""));

            } else if(message.contains("of airtime on")) {
                transactionType = "Airtime & Bundles";

                // Extract transaction amount
                Matcher amountMatcher = Pattern.compile("Ksh([\\d ,]+\\.\\d{2})+ of airtime").matcher(message);
                amountMatcher.find();
                transactionAmount = -1 * Double.parseDouble(amountMatcher.group(1).replace(",", "").replace(" ", ""));

                // Extract transaction cost
                Matcher costMatcher = Pattern.compile("Transaction cost, ?Ksh\\.?\\s*([\\d,]+(?:\\.\\d{2})?)").matcher(message);
                costMatcher.find();
                transactionCost = -1 * Double.parseDouble(costMatcher.group(1).replace(",", "").replace(" ", ""));

                // Extract transaction date and time
//                Matcher dateTimeMatcher = Pattern.compile("on (\\d{1,2}/\\d{1,2}/\\d{2,4}) at (\\d{1,2}:\\d{2} [AP]M)").matcher(message);
//                dateTimeMatcher.find();
                transactionDate = realDate;
                transactionTime = realTime;

                sender = "You";
                recipient = "Safaricom Airtime";

                // Extract balance
                Matcher balanceMatcher = Pattern.compile("New M-PESA balance is Ksh([\\d ,]+\\.\\d{2})").matcher(message);
                balanceMatcher.find();
                balance = Double.parseDouble(balanceMatcher.group(1).replace(",", "").replace(" ", ""));
            } else if(message.contains("of airtime for")) {
                transactionType = "Airtime & Bundles";

                // Extract transaction amount
                Matcher amountMatcher = Pattern.compile("Ksh([\\d ,]+\\.\\d{2})+ of airtime").matcher(message);
                amountMatcher.find();
                transactionAmount = -1 * Double.parseDouble(amountMatcher.group(1).replace(",", "").replace(" ", ""));

                // Extract transaction cost
                Matcher costMatcher = Pattern.compile("Transaction cost, ?Ksh([\\d,]+[.]?[ ]?[\\d]{2})").matcher(message);
                costMatcher.find();
                transactionCost = -1 * Double.parseDouble(costMatcher.group(1).replace(",", "").replace(" ", ""));

                // Extract transaction date and time
//                Matcher dateTimeMatcher = Pattern.compile("on (\\d{1,2}/\\d{1,2}/\\d{2,4}) at (\\d{1,2}:\\d{2} [AP]M)").matcher(message);
//                dateTimeMatcher.find();
                transactionDate = realDate;
                transactionTime = realTime;

                sender = "You";

                // Extract recipient
                Matcher recipientMatcher = Pattern.compile("for (\\d{10})").matcher(message);
                recipientMatcher.find();
                recipient = recipientMatcher.group(1);

                // Extract balance
                Matcher balanceMatcher = Pattern.compile("New\\s*balance\\s*is\\s*Ksh([\\d ,]+\\.\\d{2})").matcher(message);
                balanceMatcher.find();
                balance = Double.parseDouble(balanceMatcher.group(1).replace(",", "").replace(" ", ""));
            } else if(message.contains("from your KCB M-PESA account")) {
                transactionType = "KCB Mpesa account";

                // Extract transaction amount
                Matcher amountMatcher = Pattern.compile("You have transfered Ksh([\\d ,]+\\.\\d{2})+ from").matcher(message);
                amountMatcher.find();
                transactionAmount = Double.parseDouble(amountMatcher.group(1).replace(",", "").replace(" ", ""));

                transactionCost = 0.00;

                // Extract transaction date and time
//                Matcher dateTimeMatcher = Pattern.compile("on (\\d{1,2}/\\d{1,2}/\\d{2,4}) at (\\d{1,2}:\\d{2} [AP]M)").matcher(message);
//                dateTimeMatcher.find();
                transactionDate = realDate;
                transactionTime = realTime;

                sender = "KCB M-PESA Account";
                recipient = "You";

                // Extract balance
                Matcher balanceMatcher = Pattern.compile("New M-PESA balance is Ksh([\\d ,]+\\.\\d{2})").matcher(message);
                balanceMatcher.find();
                balance = Double.parseDouble(balanceMatcher.group(1).replace(",", "").replace(" ", ""));
            } else if(message.contains("transfered to KCB M-PESA account")) {
                transactionType = "KCB Mpesa account";

                // Extract transaction amount
                Matcher amountMatcher = Pattern.compile("Ksh([\\d ,]+\\.\\d{2})+ transfered").matcher(message);
                amountMatcher.find();
                transactionAmount = -1 * Double.parseDouble(amountMatcher.group(1).replace(",", "").replace(" ", ""));

                transactionCost = 0.00;

                // Extract transaction date and time
//                Matcher dateTimeMatcher = Pattern.compile("on (\\d{1,2}/\\d{1,2}/\\d{2,4}) at (\\d{1,2}:\\d{2} [AP]M)").matcher(message);
//                dateTimeMatcher.find();
                transactionDate = realDate;
                transactionTime = realTime;

                sender = "You";
                recipient = "KCB M-PESA Account";

                // Extract balance
                Matcher balanceMatcher = Pattern.compile("New M-PESA balance is Ksh([\\d ,]+\\.\\d{2})").matcher(message);
                balanceMatcher.find();
                balance = Double.parseDouble(balanceMatcher.group(1).replace(",", "").replace(" ", ""));
            } else if(message.contains("Your M-Shwari loan has been approved")) {
                String pattern = "([A-Z0-9]+)\\s*Confirmed\\.\\s*Your\\s*M-Shwari\\s*loan\\s*has\\s*been\\s*approved\\s*on\\s*(\\d{1,2}/\\d{1,2}/\\d{2})\\s*(\\d{1,2}:\\d{2}\\s*[AP]M)\\s*and\\s*Ksh([\\d,]+\\.\\d{2})\\s*has\\s*been\\s*deposited\\s*to\\s*your\\s*M-PESA\\s*account\\.\\s*New\\s*M-PESA\\s*balance\\s*is\\s*Ksh([\\d,]+\\.\\d{2})\\s*\\.";

                // Create a Pattern object
                Pattern regexPattern = Pattern.compile(pattern);

                // Create a Matcher object
                Matcher matcher = regexPattern.matcher(message);

                // Check if pattern matches
                if (matcher.find()) {
                    transactionCode = matcher.group(1);
                    transactionType = "Mshwari";
                    transactionDate = realDate;
                    transactionTime = realTime;
                    transactionAmount = Double.parseDouble(matcher.group(4).replace(",", ""));
                    transactionCost = 0.0;
                    balance = Double.parseDouble(matcher.group(5).replace(",", ""));
                    sender = "M-Shwari loan";
                    recipient = "You";
                }
            } else if(message.contains("repaid from M-PESA")) {
                String pattern = "([A-Z0-9]+)\\s*Confirmed\\.\\s*Loan\\s*of\\s*Ksh([\\d,]+\\.\\d{2})\\s*repaid\\s*from\\s*M-PESA\\s*on\\s*(\\d{1,2}/\\d{1,2}/\\d{2})\\s*at\\s*(\\d{1,2}:\\d{2}\\s*[AP]M)\\.\\s*Loan\\s*balance\\s*is\\s*Ksh([\\d,]+\\.\\d{2})\\.\\s*Transaction\\s*cost\\s*Kshs\\s*([\\d,]+\\.\\d{2})";

                // Create a Pattern object
                Pattern regexPattern = Pattern.compile(pattern);

                // Create a Matcher object
                Matcher matcher = regexPattern.matcher(message);

                // Check if pattern matches
                if (matcher.find()) {
                    transactionCode = matcher.group(1);
                    transactionType = "Mshwari";
                    transactionAmount = -1 * Double.parseDouble(matcher.group(2).replace(",", ""));
                    transactionDate = realDate;
                    transactionTime = realTime;
                    balance = Double.parseDouble(matcher.group(5).replace(",", ""));
                    transactionCost = Double.parseDouble(matcher.group(6).replace(",", ""));
                    sender = "You";
                    recipient = "M-Shwari loan";
                }
            } else if(message.contains("from your M-PESA account to KCB M-PESA")) {
                String pattern = "([A-Z0-9]+)\\s*Confirmed\\.\\s*Your\\s*loan\\s*repayment\\s*of\\s*Ksh([\\d,]+\\.\\d{2})\\s*from\\s*your\\s*M-PESA\\s*account\\s*to\\s*KCB\\s*M-PESA\\s*on\\s*(\\d{1,2}/\\d{1,2}/\\d{2})\\s*at\\s*(\\d{1,2}:\\d{2}\\s*[AP]M)\\s*is\\s*successful\\.\\s*Your\\s*M-PESA\\s*balance\\s*is\\s*Ksh([\\d,]+\\.\\d{2})\\.";

                // Create a Pattern object
                Pattern regexPattern = Pattern.compile(pattern);

                // Create a Matcher object
                Matcher matcher = regexPattern.matcher(message);

                // Check if pattern matches
                if (matcher.find()) {
                    transactionCode = matcher.group(1);
                    transactionType = "KCB Mpesa account";
                    transactionAmount = -1 * Double.parseDouble(matcher.group(2).replace(",", ""));
                    transactionDate = realDate;
                    transactionTime = realTime;
                    balance = Double.parseDouble(matcher.group(5).replace(",", ""));
                    transactionCost = 0.0;
                    sender = "You";
                    recipient = "KCB M-PESA loan";
                }
            } else if (message.contains("transfered to Lock Savings") ||
                    message.contains("transferred to Lock Savings") ||
                    message.contains("transferred to M-Shwari Lock Savings") ||
                    message.contains("transfered to M-Shwari Lock Savings") ||
                    message.contains("Lock Savings Account")) {

                // Adjust patterns to match variations in wording
                String patternAmount = "Ksh([\\d,]+\\.\\d{2}) (?:transfered|transferred)";
                String patternDateTime = "on (\\d{1,2}/\\d{1,2}/\\d{2,4}) at (\\d{1,2}:\\d{2} [AP]M)";
                String patternBalance = "M-PESA balance is Ksh([\\d,]+\\.\\d{2})";
                String patternTransactionCost = "Transaction cost Ksh\\.([\\d,]+\\.\\d{2})";

                // Extract transaction amount
                Matcher amountMatcher = Pattern.compile(patternAmount).matcher(message);
                if (amountMatcher.find()) {
                    transactionAmount = -1 * Double.parseDouble(amountMatcher.group(1).replace(",", ""));
                }

                // Extract transaction date and time
                Matcher dateTimeMatcher = Pattern.compile(patternDateTime).matcher(message);
                if (dateTimeMatcher.find()) {
                    transactionDate = dateTimeMatcher.group(1);
                    transactionTime = dateTimeMatcher.group(2);
                } else {
                    // Fallback to realDate and realTime if date/time is missing
                    transactionDate = realDate;
                    transactionTime = realTime;
                }

                // Extract balance
                Matcher balanceMatcher = Pattern.compile(patternBalance).matcher(message);
                if (balanceMatcher.find()) {
                    balance = Double.parseDouble(balanceMatcher.group(1).replace(",", ""));
                }

                // Extract transaction cost (optional, default to 0 if not present)
                Matcher transactionCostMatcher = Pattern.compile(patternTransactionCost).matcher(message);
                if (transactionCostMatcher.find()) {
                    transactionCost = Double.parseDouble(transactionCostMatcher.group(1).replace(",", ""));
                } else {
                    transactionCost = 0.00;
                }

                // Set transaction details
                transactionType = "Mshwari";
                sender = "You";
                recipient = "M-Shwari Lock Savings Account";
            } else if(message.contains("transferred to M-Shwari account")) {
                String patternAmount = "Ksh([\\d ,]+\\.\\d{2})+ transferred to";
                String patternCost = "Transaction cost  ?Ksh\\.?\\s*([\\d,]+(?:\\.\\d{2})?)";
                String patternDateTime = "on (\\d{1,2}/\\d{1,2}/\\d{2,4}) at (\\d{1,2}:\\d{2} [AP]M)";
                String patternBalance = "M-PESA balance is Ksh([\\d ,]+\\.\\d{2})";

                // Extract transaction amount
                Matcher amountMatcher = Pattern.compile(patternAmount).matcher(message);
                amountMatcher.find();
                transactionAmount = -1 * Double.parseDouble(amountMatcher.group(1).replace(",", "").replace(" ", ""));

                // Extract transaction cost
                Matcher costMatcher = Pattern.compile(patternCost).matcher(message);
                costMatcher.find();
                transactionCost = -1 * Double.parseDouble(costMatcher.group(1).replace(",", "").replace(" ", ""));

                // Extract transaction date and time
//                Matcher dateTimeMatcher = Pattern.compile(patternDateTime).matcher(message);
//                dateTimeMatcher.find();
                transactionDate = realDate;
                transactionTime = realTime;

                // Extract balance
                Matcher balanceMatcher = Pattern.compile(patternBalance).matcher(message);
                balanceMatcher.find();
                balance = Double.parseDouble(balanceMatcher.group(1).replace(",", "").replace(" ", ""));

                transactionType = "Mshwari";
                sender = "You";
                recipient = "M-Shwari account";
            } else if(message.contains("transferred from M-Shwari")) {
                String patternAmount = "Ksh([\\d ,]+\\.\\d{2})+ transferred from";
                String patternCost = "Transaction cost Ksh\\.?\\s*([\\d,]+(?:\\.\\d{2})?)";
                String patternDateTime = "on (\\d{1,2}/\\d{1,2}/\\d{2,4}) at (\\d{1,2}:\\d{2} [AP]M)";
                String patternBalance = "M-PESA balance is Ksh([\\d ,]+\\.\\d{2})";

                // Extract transaction amount
                Matcher amountMatcher = Pattern.compile(patternAmount).matcher(message);
                amountMatcher.find();
                transactionAmount = Double.parseDouble(amountMatcher.group(1).replace(",", "").replace(" ", ""));

                // Extract transaction cost
                Matcher costMatcher = Pattern.compile(patternCost).matcher(message);
                costMatcher.find();
                transactionCost = -1 * Double.parseDouble(costMatcher.group(1).replace(",", "").replace(" ", ""));

                // Extract transaction date and time
//                Matcher dateTimeMatcher = Pattern.compile(patternDateTime).matcher(message);
//                dateTimeMatcher.find();
                transactionDate = realDate;
                transactionTime = realTime;

                // Extract balance
                Matcher balanceMatcher = Pattern.compile(patternBalance).matcher(message);
                balanceMatcher.find();
                balance = Double.parseDouble(balanceMatcher.group(1).replace(",", "").replace(" ", ""));

                transactionType = "Mshwari";
                sender = "M-Shwari account";
                recipient = "You";
            } else if(message.contains("has been successfully reversed")) {
                transactionAmount = 0.0;
                transactionCost = 0.0;

                // Determine transaction type
                transactionType = "Reversal";


                // Extract transaction amount
                Matcher amountMatcher = Pattern.compile("Ksh([\\d,]+\\.\\d{2})+ (is debited from|is credited to)").matcher(message);
                if (amountMatcher.find()) {
                    transactionAmount = Double.parseDouble(amountMatcher.group(1).replace(",", "").replace(" ", ""));
                }

                // Set sender and recipient based on message content
                Matcher recipientMatcher = Pattern.compile("to ([^.\\n]+)").matcher(message);
                if (recipientMatcher.find()) {
                    recipient = "You";
                    sender = "Wrong recipient";
                    // transaction_amount = -1 * transaction_amount;
                } else {
                    recipient = "Wrong sender";
                    sender = "You";
                    transactionAmount = -1 * transactionAmount;
                }

                // Extract transaction date and time
//                Matcher dateMatcher = Pattern.compile("on (\\d{1,2}/\\d{1,2}/\\d{2,4})[^\\d]*(\\d{1,2}:\\d{2} ?[AP]M)").matcher(message);

                transactionDate = realDate;
                transactionTime = realTime;


                // Extract balance
                Matcher balanceMatcher = Pattern.compile("New M-PESA account balance is Ksh([\\d ,]+\\.\\d{2})").matcher(message);
                if (balanceMatcher.find()) {
                    balance = Double.parseDouble(balanceMatcher.group(1).replace(",", "").replace(" ", ""));
                }
            } else if(message.contains("Give Ksh")) {
                transactionType = "Deposit";

                transactionCost = 0.0;

                recipient = "You";

                // Extract transaction amount
                Matcher amountMatcher = Pattern.compile("Give Ksh([\\d ,]+\\.\\d{2})+ cash to").matcher(message);
                if (amountMatcher.find()) {
                    transactionAmount = Double.parseDouble(amountMatcher.group(1).replace(",", "").replace(" ", ""));
                }

                // Extract transaction date and time
//                Matcher dateTimeMatcher = Pattern.compile("On (\\d{1,2}/\\d{1,2}/\\d{2}) at (\\d{1,2}:\\d{2} [AP]M)").matcher(message);
                transactionDate = realDate;
                transactionTime = realTime;

                // Extract sender
                Matcher senderMatcher = Pattern.compile("to (.+?) New").matcher(message);
                if (senderMatcher.find()) {
                    sender = senderMatcher.group(1).replace("\u00A0", " ");  // Replace non-breaking space character
                }

                // Extract balance
                Matcher balanceMatcher = Pattern.compile("New M-PESA balance is Ksh([\\d ,]+\\.\\d{2})").matcher(message);
                if (balanceMatcher.find()) {
                    balance = Double.parseDouble(balanceMatcher.group(1).replace(",", "").replace(" ", ""));
                }
            } else if(message.contains("from Hustler Fund")) {
                transactionType = "Hustler Fund";
                transactionCost = 0.0;
                recipient = "You";

                // Extract transaction amount
                Matcher amountMatcher = Pattern.compile("Ksh([\\d ,]+\\.\\d{2})+ from").matcher(message);
                if (amountMatcher.find()) {
                    transactionAmount = Double.parseDouble(amountMatcher.group(1).replace(",", "").replace(" ", ""));
                }

                // Extract transaction date and time
//                Matcher dateTimeMatcher = Pattern.compile("on (\\d{1,2}/\\d{1,2}/\\d{4}) at (\\d{1,2}:\\d{2} [AP]M)").matcher(message);
                transactionDate = realDate;
                transactionTime = realTime;

                // Extract sender
                Matcher senderMatcher = Pattern.compile("You have received Ksh([\\d ,]+\\.\\d{2}) from (.+?) on").matcher(message);
                if (senderMatcher.find()) {
                    sender = senderMatcher.group(2);
                }

                // Extract balance
                Matcher balanceMatcher = Pattern.compile("New MPESA balance is Ksh([\\d ,]+\\.\\d{2})").matcher(message);
                if (balanceMatcher.find()) {
                    balance = Double.parseDouble(balanceMatcher.group(1).replace(",", "").replace(" ", ""));
                }
            } else if(message.contains("You have received Ksh")) {
                transactionType = "Send Money";
                transactionCost = 0.0;
                recipient = "You";

                // Extract transaction amount
                Matcher amountMatcher = Pattern.compile("You have received Ksh([\\d ,]+\\.\\d{2})").matcher(message);
                if (amountMatcher.find()) {
                    transactionAmount = Double.parseDouble(amountMatcher.group(1).replace(",", "").replace(" ", ""));
                }

                // Extract transaction date and time
//                Matcher dateTimeMatcher = Pattern.compile("on (\\d{1,2}/\\d{1,2}/\\d{2,4}) at (\\d{1,2}:\\d{2} [AP]M)").matcher(message);
                transactionDate = realDate;
                transactionTime = realTime;

                // Extract sender
                Matcher senderMatcher = Pattern.compile("from (.+?)(?: on| for)").matcher(message);
                if (senderMatcher.find()) {
                    sender = senderMatcher.group(1).replace("\u00A0", " ");
                }

                // Extract balance
                Matcher balanceMatcher = Pattern.compile("New\\s*M-PESA\\s*balance\\s*is\\s*Ksh([\\d ,]+\\.\\d{2})").matcher(message);
                if (balanceMatcher.find()) {
                    balance = Double.parseDouble(balanceMatcher.group(1).replace(",", "").replace(" ", ""));
                }
            } else if(message.contains("partially pay your outstanding Fuliza") || message.contains("fully pay your outstanding Fuliza")) {
                transactionAmount = 0.0;
                sender = "You";
                balance = 0.0;

                // Extract transaction amount
                Matcher amountMatcher = Pattern.compile("Ksh ([\\d ,]+\\.\\d{2})+ from").matcher(message);
                if (amountMatcher.find()) {
                    transactionAmount = -1 * Double.parseDouble(amountMatcher.group(1).replace(",", "").replace(" ", ""));
                }

                // Set transaction type
                transactionType = "Fuliza";

                // Set transaction cost
                transactionCost = 0.0;

                // Extract transaction date and time (assuming real_date and real_time are predefined)

                transactionDate = realDate;
                transactionTime = realTime;

                // Extract recipient
                Matcher recipientMatcher = Pattern.compile("outstanding (.+?(?=\\.))").matcher(message);
                if (recipientMatcher.find()) {
                    recipient = recipientMatcher.group(1);
                }

                // Extract balance
                Matcher balanceMatcher = Pattern.compile("M-PESA balance is Ksh([\\d ,]+\\.\\d{2})").matcher(message);
                if (balanceMatcher.find()) {
                    balance = Double.parseDouble(balanceMatcher.group(1).replace(",", "").replace(" ", ""));
                }
            }
            List<MessageData> messagesToAdd = new ArrayList<>();
            List<Transaction> transactionsToAdd = new ArrayList<>();

            if (transactionAmount != null) {
                transactionDto.setTransactionCode(transactionCode);
                transactionDto.setTransactionType(transactionType);
                transactionDto.setTransactionAmount(transactionAmount);
                transactionDto.setTransactionCost(transactionCost);
                transactionDto.setDate(LocalDate.parse(transactionDate, formatter));
                transactionDto.setTime(LocalTime.parse(transactionTime));
                transactionDto.setSender(sender);
                transactionDto.setRecipient(recipient);
                transactionDto.setBalance(balance);

                Transaction transaction = new Transaction(0, "", "", 0.0, 0.0, LocalDate.now(), LocalTime.now(), "", "", null, null, 0.0, "", 0);
                transaction.setTransactionCode(transactionCode);
                transaction.setTransactionType(transactionType);
                transaction.setTransactionAmount(transactionAmount);
                transaction.setTransactionCost(transactionCost);
                transaction.setDate(LocalDate.parse(transactionDate, formatter));
                transaction.setTime(LocalTime.parse(transactionTime));
                transaction.setSender(sender);
                transaction.setRecipient(recipient);
                transaction.setBalance(balance);
                transaction.setUserId(userAccount.getBackupUserId());
                
                // UPDATED: Normalize entity for data minimization (March 24, 2026)
                // Preserves visible digits in their positions
                // 
                // Examples:
                //   "BENARD KOECH 0723378780" → "BENARD KOECH 0723378780"
                //   "BENARD KOECH 072***8780" → "BENARD KOECH 072___8780"
                //   "BENARD KOECH 0712345678" → "BENARD KOECH 0712345678" (different person!)
                // 
                // This ensures different people with same name stay separate,
                // while same person with masked phone maintains visible digit pattern.
                String rawEntity = "";
                if(transactionAmount > 0) {
                    rawEntity = sender;
                } else if(transactionAmount < 0) {
                    rawEntity = recipient;
                }
                
                assert rawEntity != null;
                
                // Normalize entity (preserves visible digits with position awareness)
                String entity = normalizeEntity(rawEntity);
                transaction.setEntity(entity);

                // Query for existing transactions using normalized entity
                List<Transaction> transactions = transactionsDao.getStaticTransactionByEntity(entity);

                String nickname = "";
                if(!transactions.isEmpty()) {
                    if(transactions.get(0).getNickName() != null) {
                        nickname = transactions.get(0).getNickName();
                    }
                }
                MessageData message2 = new MessageData("", "", "");
                message2.setBody(messageDto.getBody());
                message2.setDate(String.valueOf(LocalDate.parse(messageDto.getDate(), formatter)));
                message2.setTime(String.valueOf(LocalTime.parse(messageDto.getTime())));
                messagesToAdd.add(message2);

                if(!nickname.isEmpty() && !nickname.isBlank()) {
                    transaction.setNickName(nickname);
                }

                TransactionInsertion transactionInsertion = new TransactionInsertion();
                transactionInsertion.addTransaction(transaction, transactionsDao, categories, categoryDao);
            }
        }


        return transactionDto;
    }
}


