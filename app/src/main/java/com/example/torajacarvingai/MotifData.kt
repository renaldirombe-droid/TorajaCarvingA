package com.example.torajacarvingai

// Struktur Data untuk 1 Motif
data class MotifToraja(
    val idModel: String,        // ID dari AI (contoh: "pa_tedong")
    val namaLayar: String,      // Nama yang cantik (contoh: "Pa'tedong")
    val deskripsiSingkat: String,
    val deskripsiLengkap: String,
    val gambar: Int,
    val aliases: List<String> = emptyList(),
    val category: String = "",  // Diisi MotifDisplayBuilder dari motif_catalog aktif; kosong utk entri statis lama
    val imageUrl: String = ""   // Diisi MotifDisplayBuilder dari motif_catalog aktif; kosong = pakai `gambar` (drawable)
)

// Database Sentral
object SumberData {
    val listMotif = listOf(
        MotifToraja(
            idModel = "pa_barana",
            namaLayar = "Pa' Barana'",
            deskripsiSingkat = "Barana' atau Beringin ; Pa' barana' adalah ukiran yang menyerupai ranting dan daun beringin. Beringin adalah pohon yang besar, tinggi dan rindang/rimbun sehingga semua yang ada dibawahnya merasakan kesejukan dan keamanan.",
            deskripsiLengkap = "Barana' atau Beringin ; Pa' barana' adalah ukiran yang menyerupai ranting dan daun beringin. Beringin adalah pohon yang besar, tinggi dan rindang/rimbun sehingga semua yang ada dibawahnya merasakan kesejukan dan keamanan. Pa' Barana' digunakan pada tongkonan layuk dan tongkonan pekamberan/ kaparengngesan.\n\nMaknanya:\n- Melambangkan kebangsawanan, status tertingi dalam masyarakat dan menjadi pengayom atau pelindung kepada semua pihak didalam masyarakat.\n- Melambangkan kewibawaan dan kekuasaan",
            gambar = R.drawable.barana,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_baranae'(Passape_Bai)",
            namaLayar = "Pa' Baranae' (Passape Bai)",
            deskripsiSingkat = "Passape atau Dipisahkan atau Bahagian Bai atau Babi Ukiran Pa' Baranae' atau Passapa Bai adalah ukiran yang menyerupai mulut babi antara rahang bawah dengan rahang atas.",
            deskripsiLengkap = "Passape atau Dipisahkan atau Bahagian Bai atau Babi Ukiran Pa' Baranae' atau Passapa Bai adalah ukiran yang menyerupai mulut babi antara rahang bawah dengan rahang atas.\n\nMaknanya:\n- Peningkatan dari tana' satu ke tana' yang lain (perpaduan dalam satu keluarga antara tana' yang satu dengan tana' yang lain).",
            gambar = R.drawable.baranae,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_bare_allo",
            namaLayar = "Pa'Barre Allo",
            deskripsiSingkat = "Barre atau Berre' atau Terbit (matahari terbit) Barre atau Bulat atau Matahari Bulat Allo atau Matahari Ukiran yang menyerupai bulatan matahari dengan pancaran sinarnya bagaikan sinar matahari yang baru terbit dipagi hari. Jenis ukiran ini ditemukan pada bagian muka dan belakang rumah ad at To ra ja p ad a p ap an a tas berbentuk segi tiga (Para Longa).",
            deskripsiLengkap = "Barre atau Berre' atau Terbit (matahari terbit) Barre atau Bulat atau Matahari Bulat Allo atau Matahari Ukiran yang menyerupai bulatan matahari dengan pancaran sinarnya bagaikan sinar matahari yang baru terbit dipagi hari. Jenis ukiran ini ditemukan pada bagian muka dan belakang rumah ad at To ra ja p ad a p ap an a tas berbentuk segi tiga (Para Longa). Biasanya diatas ukiran Pa' Barre Allo diletakkan ukiran Pa' Manuk Londong. Matahari sebagai raja siang dapat diartikan sebagai kemuliaan. Matahari dengan pancaran sinarnya diwaktu pagi bermakna kehidupan yang bersumber dari Puang Matua (Sang Pencipta), sumber kemuliaan. Pa' Barre Allo yang diletakkan diatas para longa bermakna kemaha tinggian. Ukiran Pa' Barre Allo biasa dipasang pada pembungkus mayat (Balun) untuk orang yang sundun alukna, karena orang yang sundun alukna dan dapat dibalikan pesungna, pasti akan kembali menjadi pemberi berkah kepada anak cucunya dan menjadi sumber kehidupan.\n\nMaknanya:\n- Percaya bahwa sumber kehidupan dan segala sesuatu didunia ini adalah dari Puang Matua (Tuhan Yang Maha Esa).\n- Pemilik tongkonan mempunyai kedudukan yang tertinggi dan mulia.",
            gambar = R.drawable.pa_bare_allo,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_batang_lau",
            namaLayar = "Pa' Batang Lau",
            deskripsiSingkat = "Batang atau Batang. Lau atau Labu. Ukiran ini menggambarkan batang labu, yang hanya satu batang induk, kemudian bercabang lalu dari cabang tumbuh rantingnya dan dari ranting bertunas menjadi cabang dan seterusnya hingga mencapai ratusan meter panjangnya.",
            deskripsiLengkap = "Batang atau Batang. Lau atau Labu. Ukiran ini menggambarkan batang labu, yang hanya satu batang induk, kemudian bercabang lalu dari cabang tumbuh rantingnya dan dari ranting bertunas menjadi cabang dan seterusnya hingga mencapai ratusan meter panjangnya. Jadi bagaimanapun panjangnya batang labuitu merayap kemana-mana namun tetap berhubungan dengan batang induknya.\n\nMaknanya:\n- Hubungan kekeluargaan bagaimanapun jauhnya harus tetap dipelihara dan dipupuk karena berasal dari satu leljuhur (Tongkonan) ataukah sebagai anggota satu kelompok masyarakat",
            gambar = R.drawable.batang_lau,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_bombo_uai",
            namaLayar = "Pa' Bombo Uai (I)",
            deskripsiSingkat = "Pa' Bombo Uai adalah ukiran yang menyerupai binatang air (bombo uai). Bombo uai adalah binatang yang dapat menitih air dan dapat bergerak sangat cepat.",
            deskripsiLengkap = "Pa' Bombo Uai adalah ukiran yang menyerupai binatang air (bombo uai). Bombo uai adalah binatang yang dapat menitih air dan dapat bergerak sangat cepat.\n\nMaknanya:\n- Pintar - pintarlah menitih kehidupan ini, dalam hal ini kita harus lincah, cekatan, cepat dan tepat pada tujuan.\n- Manusia harus mempunyai keterampilan dan kemampuan yang cukup dalam melaksanakan tugas dan tanggung jawab.",
            gambar = R.drawable.bombo_uai,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_bulintong_somba",
            namaLayar = "Pa' Bulintong Somba ’",
            deskripsiSingkat = "Pa' Bulintong Somba' adalah pengembangan dari ukiran. Pa' Bulintong.",
            deskripsiLengkap = "Pa' Bulintong Somba' adalah pengembangan dari ukiran. Pa' Bulintong. Bulintong Somba' atau Berudu atau Berenang dengan tenang dan menuju satu arah dalam air yang jernih dan tenang.\n\nMaknanya:\n- Rumpun keluarga (to ma' rapu) berharap semua generasi turun temurun dapat hidup dengan aman dan sentosa dalam mengarungi kehidupan ini.\n- Pada waktu masih anak-anak harus di didik dan diarahkan dengan baik sehingga apabila sudah dewasa dapat memilih jalan hidupnya sendiri.",
            gambar = R.drawable.bulintong,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_bulu_londong",
            namaLayar = "Pa' Bulu Londong",
            deskripsiSingkat = "Bulu atau Bulu Londong atau Ayam Jantan. Ukiran ini menyerupai rumbai ayam jantan.",
            deskripsiLengkap = "Bulu atau Bulu Londong atau Ayam Jantan. Ukiran ini menyerupai rumbai ayam jantan. Ada pepatah mengatakan : Ayam dikenal karena bulunya, manusia dikenal karena tingkah lakunya. Dimuka/telah dijelaskan tentang arti dan makna londong (ayam jantan) yaitu kepemimpinan, keberanian dan kepastian hukum. Dengan melihat bulu ayam maka kita dapat memberi nama pada ayam misalnya Buri', Koro, Pute, dan lain-lain. Demikian pula manusia, dengan mengetahui atau melihat tingkah lakunya maka kita sudah bisa mengenal tipe kepemimpinan dan kemampuan seorang pemimpin. Pa' bulu londong biasa di garunggang atau diukir tembus (lihat arti pa' garunggang pada makna-makna khusus).\n\nMaknanya:\n- Bulu rumbai menghiasi ayam jantan demikian pula keperkasaan dan kewibawaan menyertai seorang pemimpin dan lelaki pemberani",
            gambar = R.drawable.bulu_londong,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_dadu",
            namaLayar = "Pa’ Dadu",
            deskripsiSingkat = "Dadu yaitu sejenis benda segi empat sama sisi yang biasa digunakan untu bermain judi. Permainan dadu di Toraja merupakan sejenis judi yang digemari oleh sebagian masyarakat.",
            deskripsiLengkap = "Dadu yaitu sejenis benda segi empat sama sisi yang biasa digunakan untu bermain judi. Permainan dadu di Toraja merupakan sejenis judi yang digemari oleh sebagian masyarakat. Akibat permainan dadu, maka banyak orang jatuh melarat dan menderita.\n\nMaknanya:\n- Merupakan pesan atau amanah kepada anak cucu supaya jangan bermain dadu (judi) karena sangat berbahaya dan merugikan",
            gambar = R.drawable.dadu,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_don_paria",
            namaLayar = "Pa' Daun Paria",
            deskripsiSingkat = "Daun artinya daun “Paria” artinya sayur paria. Kita maklumi bahwa paria ini terkenal dengan tanaman pahit.",
            deskripsiLengkap = "Daun artinya daun “Paria” artinya sayur paria. Kita maklumi bahwa paria ini terkenal dengan tanaman pahit. Baik daun maupun buah dapat dijadikan sayur-sayuran, selain itu daunnya dapat pula dijadikan obat batuk, cacar, dan lain-lain.\n\nMaknanya:\n- Kita tidak boleh berhati pahit atau menyakiti hati sesama manusia.\n- Sesuatu yang pahit, kalau itu adalah obat (dapat menyembuhkan) harus ditelan. Walaupun menyakitkan, kalau itu adalah nasihat/ petunjuk yang akan membawa kita keluar dan suatu masaalah dan atau mendatangkan suatu kebaikan harus diterima",
            gambar = R.drawable.don_paria,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_don_bolu",
            namaLayar = "Pa' Don Bolu (Daun Sirih)",
            deskripsiSingkat = "Ukiran yang menyerupai daun sirih, melambangkan keramahtamahan, persaudaraan erat, tanda penghormatan, serta ketaatan spiritual.",
            deskripsiLengkap = "Pa' Don Bolu berasal dari kata 'Don' (daun) dan 'Bolu' (sirih).\n\nBagi masyarakat Toraja, sirih memiliki kedudukan yang sangat penting dengan dua manfaat utama:\n1. Sebagai bahan sesajen (Ma' pesung) dalam upacara adat ritual untuk memuja Puang Matua, Deata, dan To Membali Puang.\n2. Untuk dikunyah (Makan Sirih/Dipapangngan) sebagai simbol persahabatan, pergaulan luas, dan tanda kehormatan.\n\nSaat menyuguhkan sirih kepada tamu, biasanya disertai dengan ungkapan adat:\n\"Pangnganmo sonda maliU.\nSolonna pengkaboro'ki Kisorong kisorong mati'\nKiala tanda mala'bi'\nRandepala'itoda Kiporannu matoto'i\"\n\nMakna filosofis dari motif ini meliputi:\n- Pemimpin yang Santun: Melambangkan sosok pemimpin yang memiliki pergaulan luas, kerabat banyak, serta menghormati siapa pun.\n- Hubungan Sosial: Pesan agar kita senantiasa bersahabat dan menjaga kerukunan dengan semua orang.\n- Ketaatan Spiritual: Melambangkan kepatuhan dan kepercayaan kepada Tuhan Yang Maha Esa (Puang Matua), serta lambang kehormatan bagi pemilik Tongkonan.",
            gambar = R.drawable.don_bolu,
            aliases = listOf("pa daun bolu", "pa don bolu", "daun sirih", "sirih", "papangngan", "ma pesung")
        ),
        MotifToraja(
            idModel = "pa_doti",
            namaLayar = "Pa’ Doti Langi’",
            deskripsiSingkat = "Doti atau Ilmu (hitam) ; Doti atau Salego (tedong salego) ; Doti atau Baik atau Cantik Langi' atau Langit. Ukiran berupa palang yang berjejer- jejer dan ditengah-tengah ada semacam bintang bersinar bagaikan bintang diatas langit.",
            deskripsiLengkap = "Doti atau Ilmu (hitam) ; Doti atau Salego (tedong salego) ; Doti atau Baik atau Cantik Langi' atau Langit. Ukiran berupa palang yang berjejer- jejer dan ditengah-tengah ada semacam bintang bersinar bagaikan bintang diatas langit.\n\nMaknanya:\n- Kepintaran, prestasi yang tinggi, dan kearifan serta ketenangan.\n- Mempunyai cita-cita yang tinggi, pemikiran yang cemerlang jauh kedepan.\n- Wanita bangsawan, mempunyai kasta tinggi.",
            gambar = R.drawable.doti,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_doti_pandin",
            namaLayar = "Pa’ Doti Pandin",
            deskripsiSingkat = "Doti atau Kerbau belang atau Cantik atau Ilmu hitam Pandin atau Pemuda ganteng Pa' Doti Pandin adalah pengembangan dari ukiran Pa' Doti Langi'.",
            deskripsiLengkap = "Doti atau Kerbau belang atau Cantik atau Ilmu hitam Pandin atau Pemuda ganteng Pa' Doti Pandin adalah pengembangan dari ukiran Pa' Doti Langi'.\n\nMaknanya:\n- Melambangkan gadis bangsawan yang dicintai oleh banyak pemuda. Ungkapan toraja mengatakan iko manna raka pandin assa'na bunga-bunga budapa ia pandin mane tarran buanna",
            gambar = R.drawable.doti_pandin,
            aliases = listOf("pa_sekong_pandin")
        ),
        MotifToraja(
            idModel = "pa_erong",
            namaLayar = "Pa’ Erong",
            deskripsiSingkat = "Erong bagi masyarakat Toraja adalah sejenis peti yang setiap waktu dapat dibuka menurut adat untuk menyimpan tulang belulang dari satu rumpun keluarga. Jadi erong adalah peti tempat mengumpulkan tulang-tulang orang mati dalam satu rumpun yang biasa disimpan digua-gua.",
            deskripsiLengkap = "Erong bagi masyarakat Toraja adalah sejenis peti yang setiap waktu dapat dibuka menurut adat untuk menyimpan tulang belulang dari satu rumpun keluarga. Jadi erong adalah peti tempat mengumpulkan tulang-tulang orang mati dalam satu rumpun yang biasa disimpan digua-gua. Pada saat tulang disimpan untuk disatukan dengan tulang yang sudah tersimpan, terlebih dahulu, harus melalui upacara menurut adat setempat. Benda ini hanya dimiliki oleh, orang-orang bangsawan diToraja. Bentuk erong ada bermacam-macam yaitu seperti kerbau, babi dan ada pula yang menyerupai perahu. Umumnya erong diukir seperti Pa' erong ini.\n\nMaknanya:\n- Orang Toraja percaya bahwa dengan mengukir erong, arwah orang yang sudah meninggal akan merasa diperhatikan dan diharapkan aka memberkati anak cucu dan semua kaum keluarga.\n- Melambangkan kebangsawanan.",
            gambar = R.drawable.erong,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_gayang",
            namaLayar = "Pa' Gayang",
            deskripsiSingkat = "Gaang (gayang) adalah ukiran yang menyerupai keris emas. Gayang adalah keris yang bukannya digunakan sebagai senjata melainkan lebih bermakna sebagai harta kemuliaan yang dipusakai turun temurun.",
            deskripsiLengkap = "Gaang (gayang) adalah ukiran yang menyerupai keris emas. Gayang adalah keris yang bukannya digunakan sebagai senjata melainkan lebih bermakna sebagai harta kemuliaan yang dipusakai turun temurun. Bukannya mata dari keris tersebut yang diutamakan melainkan sarungnya, karena sarungnya dari emas yang berukir. Gaang dipakai dalam upacara rambu solo' dan atau rambu tuka'. Dapat, digunakan untuk dekorasi dan dapat pula dipakai oleh penaripenari atau wanita.\n\nMaknanya:\n- Bagi Orang Toraja gayang melambangkan laki-laki yang mulia, kaya, dan bangsawan serta bijak (jelas dalam ungkapan) : “ _Kenna tang manarang gayang kenna tang pande Sarapang_ ”",
            gambar = R.drawable.gayang,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_kadang_pao",
            namaLayar = "Pa' Kadang Pao",
            deskripsiSingkat = "Kadang atau Kait atau Jolok Pao atau Mangga Pa' Kadang Pao adalah ukiran yang menyerupai alat penjolok (pengait) mangga adalah",
            deskripsiLengkap = "Kadang atau Kait atau Jolok Pao atau Mangga Pa' Kadang Pao adalah ukiran yang menyerupai alat penjolok (pengait) mangga adalah\n\nMaknanya:\n- Kita harus mempunyai kreasi atau usaha sebagai upaya untuk mendapatkan keuntungan atau hasil yang diharapkan.",
            gambar = R.drawable.kadang_pao,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_kapu_baka",
            namaLayar = "Pa' Kapu' Baka",
            deskripsiSingkat = "Kapu' atau Ikat ; Baka atau Bakul ; Kapu' Baka atau Pengikat bakul tempat menyimpan perhiasan dan harta kekayaan rumah. Pa' kapu' baka adalah ukiran yang menyerupai simpulan-simpulan penutup bakul.",
            deskripsiLengkap = "Kapu' atau Ikat ; Baka atau Bakul ; Kapu' Baka atau Pengikat bakul tempat menyimpan perhiasan dan harta kekayaan rumah. Pa' kapu' baka adalah ukiran yang menyerupai simpulan-simpulan penutup bakul. Baka (Baka Bua) adalah tempat menyimpan harta benda bagi orang-orang tua dahulu di Toraja, sebelum ada peti, lemari atau koper. Simpulan-simpulan yang disimbolkan dalam ukiran ini benar-benar rapih sehingga ujung simpulan dari tali tidak kelihatan. Bagi yang empunya merupakan rahasia, sehingga kalau simpulan rahasia ini telah berubah berarti sudah ada orang lain yang telah mengambil sesuatu dari dalam bakul itu.\n\nMaknanya:\n- Baka bua adalah tempat menyimpan harta kekayaan rumah, (benda- benda pusaka tongkonan). Jadi ukiran pa' kapu' baka melambangkan kekayaan dan kebangsawanan.\n- Simpul rahasia melambangkan bahwa pemilik rumah memiliki pola kepemimpinan dan pola hidup yang sukar ditiru atau tidak dimiliki oleh orang lain.\n- Memelihara rahasia-rahasia kekeluargaan.",
            gambar = R.drawable.kapu_baka,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_komba_kalua",
            namaLayar = "Pa' Boko' Komba Kalua’",
            deskripsiSingkat = "Nama ukiran ini terdiri dari kata- kata boko' artinya belakang, komba artinya gelang, dan kalua' artinya luas atau besar atau lebar. Jadi pa' komba kalua' yaitu perhiasan gelang dari emas dan berbentuk manik-manik yang tersusun rapi menurut arsitektur khas Toraja.",
            deskripsiLengkap = "Nama ukiran ini terdiri dari kata- kata boko' artinya belakang, komba artinya gelang, dan kalua' artinya luas atau besar atau lebar. Jadi pa' komba kalua' yaitu perhiasan gelang dari emas dan berbentuk manik-manik yang tersusun rapi menurut arsitektur khas Toraja.\n\nMaknanya:\n- Melambangkan kewibawaan dan kebesaran bagi bangsawan wanita Toraja.",
            gambar = R.drawable.komba_kalua,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_limbongan",
            namaLayar = "Pa' Limbongan",
            deskripsiSingkat = "Limbongan atau Nama orang atau Ne' Limbongan Limbong atau Tenang Limbong atau Penampungan ikan berupa lobang besar yang sengaja dibuat dalam petak sawah yang kadang-kadang tidak pernah kering airnya. Dahulu kala Ne' Limbongan adalah arsitektur Toraja dan seorang pengukir pada j amannya.",
            deskripsiLengkap = "Limbongan atau Nama orang atau Ne' Limbongan Limbong atau Tenang Limbong atau Penampungan ikan berupa lobang besar yang sengaja dibuat dalam petak sawah yang kadang-kadang tidak pernah kering airnya. Dahulu kala Ne' Limbongan adalah arsitektur Toraja dan seorang pengukir pada j amannya. Ukiran yang mirip pa' barre allo ini konon kabarnya adalah hasil ciptaan (kreasi) dari Ne' Limbongan. Itulah sebabnya ukiran ini diberi nama Pa' Limbongan (Ne' Limbongan).\n\nMaknanya:\n- Kita harus menghargai kreasi atau daya cipta seseorang,\n- Melambangkan seseorang yang selalu penuh dengan ide, selalu berkreasi dan menjadi tumpuan harapan orang banyak.",
            gambar = R.drawable.ne_limbong,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_lolo_tabang",
            namaLayar = "Pa' Lolo Tabang",
            deskripsiSingkat = "Lolo tabang terdiri dari kata lolo artinya pucuk dan tabang artinya lenjuang adalah sejenis tumbuhan di Toraja yang dapat digunakan sebagai obat. Ukiran ini menyempai pucuk daun lenjuang.",
            deskripsiLengkap = "Lolo tabang terdiri dari kata lolo artinya pucuk dan tabang artinya lenjuang adalah sejenis tumbuhan di Toraja yang dapat digunakan sebagai obat. Ukiran ini menyempai pucuk daun lenjuang.\n\nMaknanya:\n- Simbol upacara rambu tuka' (tertinggi), orang Toraja biasa menyebut unnangkaran tabang artinya mengangkat tabang atau melaksanakan upacara rambu tuka'.\n- Melambangkan kegembiraan atau kesyukuran. Ada ungkapan yang mengatakan, “Bendan na' situang tabang, Tulangda' na' sitonda kaparannuan” artinya “Saya berdiri ditengah-tengah hadirin dalam kegembiraan dan kesyukuran”\n- Lambang kebangsawanan dan kewibawaan (orang Toraja menyatakan tona tuoi tabang boko'na) artinya orang yang tumbuh lenjuang diatas punggungnya.",
            gambar = R.drawable.lolo_tabang,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_manuk_londong",
            namaLayar = "Pa' Manuk Londong",
            deskripsiSingkat = "Manuk atau Ayam ; Londong atau Jantan Pa' manuk londong adalah ukiran berupa avam jantan, biasanya terdapat pada bagian muka dan belakang rumah adat Toraja pada papan atas berbentuk segitiga (Para Longa). Biasanya ukiran ayam jantan diletakkan diatas Pa' Barre Allo.",
            deskripsiLengkap = "Manuk atau Ayam ; Londong atau Jantan Pa' manuk londong adalah ukiran berupa avam jantan, biasanya terdapat pada bagian muka dan belakang rumah adat Toraja pada papan atas berbentuk segitiga (Para Longa). Biasanya ukiran ayam jantan diletakkan diatas Pa' Barre Allo.\n\nMaknanya:\nOleh masyarakat Toraja, londong (ayam jantan) mempunyai beberapa makna, baik makna sosiologis maupun makna spiritual antara lain: .\n- Melambangkan pemimpin yang arif bijaksana dan mampu menyatukan pendapat dari semua unsur dan golongan dalam masyarakat (manarang pakorok londong pande metinti saungan)\n- Dapat dipercaya oleh karena pintar, pemahaman dan intuisinya tepat serta selalu mengatakan apa yang benar itu benar dan apa yang salah itu salah (Manarang ussuka' bongi ungkararoi malillin).\n- Bermakna adanya hukum, dalam arti dapat dan mampu menyelesaikan persoalan dengan jujur, adil dan bijaksana (Buri' ma' belo tadi, untanda katonganan unterak sanda salunna.\n- Melambangkan keberanian dan dapat/mampu berbuat segala sesuatu (londongna muane).",
            gambar = R.drawable.pa_manuk_londong,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_ne_limbongan",
            namaLayar = "Pa' Ne' Limbongan",
            deskripsiSingkat = "Limbongan atau Nama orang atau Ne' Limbongan Limbong atau Tenang Limbong atau Penampungan ikan berupa lobang besar yang sengaja dibuat dalam petak sawah yang kadang-kadang tidak pernah kering airnya. Dahulu kala Ne' Limbongan adalah arsitektur Toraja dan seorang pengukir pada j amannya.",
            deskripsiLengkap = "Limbongan atau Nama orang atau Ne' Limbongan Limbong atau Tenang Limbong atau Penampungan ikan berupa lobang besar yang sengaja dibuat dalam petak sawah yang kadang-kadang tidak pernah kering airnya. Dahulu kala Ne' Limbongan adalah arsitektur Toraja dan seorang pengukir pada j amannya. Ukiran yang mirip pa' barre allo ini konon kabarnya adalah hasil ciptaan (kreasi) dari Ne' Limbongan. Itulah sebabnya ukiran ini diberi nama Pa' Limbongan (Ne' Limbongan).\n\nMaknanya:\n- Kita harus menghargai kreasi atau daya cipta seseorang,\n- Melambangkan seseorang yang selalu penuh dengan ide, selalu berkreasi dan menjadi tumpuan harapan orang banyak.",
            gambar = R.drawable.ne_limbong,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_papan_kandaure",
            namaLayar = "Pa’ Pa’pak (Papan) Kandaure",
            deskripsiSingkat = "Papan atau Sebidang atau Sebilah atau Selembar papan Kandaure atau Perhiasan tradisional Toraja yang dibuat dari butiran manik-manik yang beraneka ragam warnanya. Kandaure terdiri •.",
            deskripsiLengkap = "Papan atau Sebidang atau Sebilah atau Selembar papan Kandaure atau Perhiasan tradisional Toraja yang dibuat dari butiran manik-manik yang beraneka ragam warnanya. Kandaure terdiri •. dari badan dan rumbai. Badan inilah yang disebut papan kandaure. Benda ini hanya dimiliki oleh orang-orang bangsawan di Toraja.\n\nMaknanya:\n- Rumpun keluarga dalam kehidupan kiranya selalu bersatu dalam satu mata rantai bagaikan butir-butir manik-manik tetap bersatu dalam seutas benang dan dapat menjadi berkat bagi orang lain.\n- Melambangkan wanita bangsawan dan berwibawa .",
            gambar = R.drawable.papan_kandaure,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_pollo_gayang",
            namaLayar = "Pa' Pollo' Gayang",
            deskripsiSingkat = "Pollo atau Pantat atau Ujung bawah Gaang Keris Emas. Pa' Pollo' Gaang adalah ukiran yang menyerupai polio' gang (ujung sebelah bawah dari keris emas).",
            deskripsiLengkap = "Pollo atau Pantat atau Ujung bawah Gaang Keris Emas. Pa' Pollo' Gaang adalah ukiran yang menyerupai polio' gang (ujung sebelah bawah dari keris emas). Polio' Gaang ini juga adalah merupakan bagian dari gaang (keris emas).\n\nMaknanya:\n- Oleh karena polio' gaang adalah bagian dari gang (keris emas) maka maknanya sama dengan ukiran pa' gaang yaitu melambangkan wanita yang mulia, kaya, dan bangsawan serta bijak.",
            gambar = R.drawable.pollo_gayang,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_rangga_ulu",
            namaLayar = "Pa' Rangga Ulu",
            deskripsiSingkat = "Rangga atau Banyak Ulu atau Kepala; Dalam ungkapan Toraja ada ungkapan: - Rangga Inaa artinya orang pintar dan bijaksana. - Rangga Lila artinya orang pintar berbicara.",
            deskripsiLengkap = "Rangga atau Banyak Ulu atau Kepala; Dalam ungkapan Toraja ada ungkapan: - Rangga Inaa artinya orang pintar dan bijaksana. - Rangga Lila artinya orang pintar berbicara.\n\nMaknanya:\n- Melambangkan keturunan yang bijak, pintar dan bijaksana",
            gambar = R.drawable.rangga_ulu,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_re_po",
            namaLayar = "Pa' Re'po (Pang Re'po)",
            deskripsiSingkat = "Re'po artinya menari lincah sambil melipat lutut dalam bentuk siku-siku.",
            deskripsiLengkap = "Re'po artinya menari lincah sambil melipat lutut dalam bentuk siku-siku.\n\nMaknanya:\n- Melambangkan orang yang cepat tanggap terhadap masalah- masalah sosial atau peka terhadap penderitaan dan kebutuhan- kebutuhan orag lain atau masyarakat (berjiwa atau berwatak social)\n- Melambangkan kebersamaan dan kegotong royongan bagi masyarakat Toraja, segala sesuatu jika dikerjakan bersama-sama pasti menjadi ringan dan lancar.",
            gambar = R.drawable.re_po,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_sekong_kandaure",
            namaLayar = "Pa' Sekong Kandaure",
            deskripsiSingkat = "Sekong atau Lekuk yang menyudut membentuk garis siku-siku. Kandaure - Perhiasan (benda) tradisional Toraja yang sangat berharga",
            deskripsiLengkap = "Sekong atau Lekuk yang menyudut membentuk garis siku-siku. Kandaure - Perhiasan (benda) tradisional Toraja yang sangat berharga\n\nMaknanya:\n- Melambangkan bahwa perjalanan hidup ini sangat berliku-liku, untu itu kita harus bekerja keras dan penuh kebijakan untuk menempuh hidup ini.\n- Melambangkan kecepatan (bagaikan kilat) dan kedahsyatan seseorang",
            gambar = R.drawable.sekong_kandaure,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_sussuk",
            namaLayar = "Pa'Sussu'",
            deskripsiSingkat = "Pa’Sussu’ ; Sussu - Garis, Goresan. Pa' sussu' adalah ukiran yang berbentuk garis-garis sejajar tanpa variasi dan tidak diberi warna.",
            deskripsiLengkap = "Pa’Sussu’ ; Sussu - Garis, Goresan. Pa' sussu' adalah ukiran yang berbentuk garis-garis sejajar tanpa variasi dan tidak diberi warna. Pa' Sussu’ adalah ukiran yang sangat sederhana tetapi maknanya sangat dalam, karena Pa' Sussu' adalah satu ukiran dasar (garonto' passura').\n\nMaknanya:\n- Pa' sussu' melambangkan Tongkonan yang bersangkutan sangat berperan didalam menentukan kebijakan dasar (dasar kehidupan) dalam wilayah adat yang bersangkutan.\n- Lambang kesatuan masyarakat yang demokratis.",
            gambar = R.drawable.sussuk,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_tanduk_rape",
            namaLayar = "Pa’ Tanduk Ra'pe",
            deskripsiSingkat = "Tanduk atau Tanduk ; Ra'pe atau Tanduk yang pangkalnya melendut kebawah dan ujungnya melengkung ke atas. Bagi kerbau, tanduk adalah merupakan perisai atau alat untuk melawan dalam rangka melindungi diri atau menyerang lawan.",
            deskripsiLengkap = "Tanduk atau Tanduk ; Ra'pe atau Tanduk yang pangkalnya melendut kebawah dan ujungnya melengkung ke atas. Bagi kerbau, tanduk adalah merupakan perisai atau alat untuk melawan dalam rangka melindungi diri atau menyerang lawan. Tanduk ra'pe adalah ukiran yang menyerupai tanduk kerbau yang Panjang dan sangat indah. Ukiran pa'tanduk ra'pe biasa dihiasi dengan uliran Pa' bulu londong.Ukiran ini biasa dilukiskan pada papan atas (itido' para) pada pinggir sebelah bawah.\n\nMaknanya:\n- Tanduk ra'pe (balian) melambangkan bangsawan yang berwibawa, berkuasa tetapi bijaksana.",
            gambar = R.drawable.tanduk_rape,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_tangke_lumu",
            namaLayar = "Pa' Tangke Lumu’",
            deskripsiSingkat = "Tangke atau Cabang atau Carang Lumu' atau Lumut. Ukiran ini menyerupai carang carang tumbuhan lumut yang hidup didalam air.",
            deskripsiLengkap = "Tangke atau Cabang atau Carang Lumu' atau Lumut. Ukiran ini menyerupai carang carang tumbuhan lumut yang hidup didalam air. Lumut melambangkan sawah yang luas, subur dan tidak pernah kering (bukan tadah hujan) dan hasilnya berlipat ganda. Kehidupan lumut dalam air selalu berkaitan, tidak pemah putus, berkaitan satu dengan yang lain.\n\nMaknanya:\n- Melambangkan kekayaan dan kemakmuran.\n- Diharapkan kiranya kaum keluarga, anak cucu turun temumn selalu memelihara persatuan dan kesatuan dan senantiasa berada dalam satu mata rantai yang tak terpisahkan baik yang dekat (berdomisili dalam negeri) maupun yang jauh (merantau di negeri orang).",
            gambar = R.drawable.tangke_lumu,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "pa_tangki_pattung",
            namaLayar = "Pa' Tangki' Pattung",
            deskripsiSingkat = "Tangki' atau Alat semacam paku yang biasanya ditanamkan disebuah tiang dengan, maksud supaya kuat berkaitan. Ditangki' atau Supaya kuat berpegangan Pattung atau Bambu besar.",
            deskripsiLengkap = "Tangki' atau Alat semacam paku yang biasanya ditanamkan disebuah tiang dengan, maksud supaya kuat berkaitan. Ditangki' atau Supaya kuat berpegangan Pattung atau Bambu besar. Ukiran seperti ini merupakan hiasan pada pegangan (tangkai) cangkir yang dibuat dari bambu. Umumnya dahulu tiap keluarga bangsawan di Toraja mempunyai gelas tradisional dari bambu, baik untuk dipakai se.ndiri maupun untuk tamu.\n\nMaknanya:\n- Rasa kekeluargaan diantara to ma'rapu (dari satu tongkonan) harus senantiasa dipupuk agar senantiasa tercipta persatuan dan kesatuan (kekeluargaan selalu erat dan kuat).\n- Melambangkan kebesaran bagi bangsawan Toraja.",
            gambar = R.drawable.tangki_pattung,
            aliases = listOf("pa_ulu_karua")
        ),
        MotifToraja(
            idModel = "pa_tedong",
            namaLayar = "Pa'Tedong",
            deskripsiSingkat = "## Tedong atau Kerbau Ukiran ini biasa dilukiskan pada papan besar teratas (indo' para) dan pada dinding-dinding penyanggah badan rumah (manangnga banua), Bagi masyarakat Toraja kerbau adalah hewan paling tinggi nilai dan statusnya, untuk itu bagi masyarakat Toraja kerbau dijadikan sebagai standart/ukuran nilai dari semua harta / aset kekayaan",
            deskripsiLengkap = "## Tedong atau Kerbau Ukiran ini biasa dilukiskan pada papan besar teratas (indo' para) dan pada dinding-dinding penyanggah badan rumah (manangnga banua), Bagi masyarakat Toraja kerbau adalah hewan paling tinggi nilai dan statusnya, untuk itu bagi masyarakat Toraja kerbau dijadikan sebagai standart/ukuran nilai dari semua harta / aset kekayaan\n\nMaknanya:\n- Ukiran ini bermakna sebagai lambang kesejahteraan dan kekayaan bagi masyarakat Toraja.\n- Melambangkan kebangsawanan.",
            gambar = R.drawable.tedong,
            aliases = emptyList()
        ),
        MotifToraja(
            idModel = "Pa_tedong_tumuru",
            namaLayar = "Pa' Tedong Tumuru",
            deskripsiSingkat = "Tedong atau Kerbau Tumuru atau Menderum. Tumuru artinya berjalan tanpa menghiraukan keadaan sekeliling.",
            deskripsiLengkap = "Tedong atau Kerbau Tumuru atau Menderum. Tumuru artinya berjalan tanpa menghiraukan keadaan sekeliling. Tumuru juga berarti kerbau yang duduk di dalam air, sambil kepalanya diatas permukaan air (kepala mencuat atau muncul diatas permukaan air)\n\nMaknanya:\n- Kita harus peka atau selalu memperhatikan keadaan disekeliling kita.",
            gambar = R.drawable.tedong_tumuru,
            aliases = listOf("pa_bungkang_tasik", "tedong_tumuru", "tedong tumuru", "Pa_Tumuru")
        ),
        MotifToraja(
            idModel = "pa_tangke_lumu_ditoke",
            namaLayar = "Pa' Tangke Lumu' Ditoke'",
            deskripsiSingkat = "Ukiran berbentuk tangkai lumut yang digantung, melambangkan kekayaan, kemakmuran, serta ikatan persatuan keluarga yang tak terpisahkan meski terpisah jarak.",
            deskripsiLengkap = "Secara harfiah, 'Tangke' berarti tangkai atau cabang, 'Lumu'' berarti lumut, dan 'Ditoke'' berarti digantung. Ukiran ini merupakan kreasi pengembangan langsung dari motif dasar Pa' Tangke Lumu'.\n\nVisual ukiran yang menyerupai jalinan tangkai lumut yang menjuntai atau digantung ini menyimpan pesan mendalam tentang hubungan kekeluargaan.\n\nMakna filosofis dari motif ini meliputi:\n1. Kekayaan & Kemakmuran: Simbol kelimpahan rezeki yang tumbuh subur seperti lumut di alam bebas.\n2. Mata Rantai Persatuan: Harapan agar seluruh kaum keluarga, anak cucu, dan generasi turun-temurun senantiasa memelihara persatuan dan kesatuan.\n3. Ikatan Perantau: Mengingatkan bahwa keluarga adalah satu mata rantai yang tak terpisahkan, mengikat erat kebersamaan baik bagi kerabat yang tinggal di kampung halaman (dalam negeri) maupun yang sedang pergi merantau di negeri orang.",
            gambar = R.drawable.lumu_ditoke,
            aliases = listOf("pa tangke lumu ditoke", "tangke lumu ditoke", "tangkai lumut digantung", "lumu ditoke")
        )
    )

    // Fungsi bantuan untuk mencari motif berdasarkan ID dari AI
    fun cariMotif(id: String): MotifToraja? {
        // Bersihkan ID dari spasi dan jadikan huruf kecil semua
        val idBersih = id.trim().lowercase()

        return listMotif.find {
            // Periksa idModel utama dan semua alias
            it.idModel.trim().lowercase() == idBersih ||
            it.aliases.any { alias -> alias.trim().lowercase() == idBersih }
        }
    }
}
