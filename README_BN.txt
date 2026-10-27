Universal App Locker — প্রাথমিক Android Studio প্রজেক্ট

এই প্রজেক্টটি একটি প্রাথমিক প্রোটোটাইপ; সম্পূর্ণ পরীক্ষা করা APK নয়।

ফিচার:
- ফোনে launcher-এ দেখা যায় এমন অ্যাপের তালিকা
- প্রতিটি অ্যাপের জন্য আলাদা দৈনিক সীমা (মিনিটে)
- সীমা শেষ হলে PIN স্ক্রিন দেখানোর চেষ্টা
- PIN সেট করার অপশন

সেটআপ:
1. Android Studio ইনস্টল শেষ হলে এটি খুলুন।
2. Open নির্বাচন করে এই ZIP extract করা ফোল্ডারটি নির্বাচন করুন।
3. Gradle sync শেষ হতে দিন। SDK 35 ডাউনলোড চাইলে অনুমতি দিন।
4. ফোনে Developer options ও USB debugging চালু করে USB দিয়ে কানেক্ট করুন।
5. Run চাপুন।
6. অ্যাপ খুলে "Usage Access" এবং "Accessibility Service" সেটিংসে গিয়ে Universal App Locker চালু করুন।
7. PIN সেট করুন। প্রতিটি অ্যাপের পাশে দৈনিক মিনিট লিখে Save চাপুন। 0 দিলে সেই অ্যাপের সীমা বন্ধ থাকবে।

সতর্কতা ও সীমাবদ্ধতা:
- এটি একটি starter prototype; এখানে Usage Access permission screen-এর লিংক রাখা হয়েছে, কিন্তু বর্তমান usage query-ভিত্তিক নির্ভুল হিসাব নয়—Accessibility event থেকে foreground time আনুমানিকভাবে হিসাব করে।
- Android/Oppo battery optimization, service বন্ধ হয়ে যাওয়া, system app behaviour, split-screen বা permission পরিবর্তনের কারণে tracking মিস হতে পারে।
- PIN কেবল লোকাল preferences-এ সাধারণভাবে রাখা হয়েছে; production app-এর জন্য নিরাপদ hashing/keystore, recovery design, foreground-service handling, Android version testing ও bypass testing দরকার।
- PIN দিয়ে আজকের জন্য unlock feature-এর bypass enforcement এখনও পুরোপুরি সম্পূর্ণ নয়; এটি প্রাথমিক UI/prototype।
- Android অ্যাপকে পুরোপুরি bypass-proof করা যায় না; Settings থেকে permission বন্ধ করা বা app data clear করা সম্ভব হতে পারে।
- এই ZIP-টি compile বা বাস্তব ফোনে পরীক্ষা করা হয়নি। প্রথমে Android Studio-তে build error এলে error message পাঠান।
