package com.example.data.model

import java.text.Normalizer

object RecipeCatalog {

    val ingredientGroups: List<IngredientGroup> = listOf(
        IngredientGroup(
            title = "Carnes y proteínas",
            items = listOf("Pollo", "Res", "Cerdo", "Pescado", "Atún", "Huevo", "Pavo")
        ),
        IngredientGroup(
            title = "Carbohidratos",
            items = listOf("Arroz", "Avena", "Pan", "Cereales", "Tortilla", "Papa", "Frijoles")
        ),
        IngredientGroup(
            title = "Pastas",
            items = listOf("Penne", "Fettuccini", "Espagueti", "Fusilli")
        ),
        IngredientGroup(
            title = "Verduras y extras",
            items = listOf("Brócoli", "Espinaca", "Tomate", "Cebolla", "Queso", "Aguacate", "Champiñones", "Yogurt")
        )
    )

    val recipes: List<RecipeOption> = listOf(
        // === BÁSICO ===
        RecipeOption(
            id = "basic_pollo_arroz",
            title = "Pollo a la plancha con arroz y brócoli",
            difficulty = RecipeDifficulty.BASIC,
            prepMinutes = 20,
            matchTags = listOf("pollo", "arroz", "brocoli", "cebolla"),
            ingredients = listOf(
                "160 g de pechuga de pollo en filete",
                "150 g de arroz cocido (o 60 g en crudo)",
                "120 g de brócoli en ramilletes",
                "1 cdta de aceite de oliva, sal, pimienta y ajo en polvo"
            ),
            steps = listOf(
                "Cuece el arroz en agua hirviendo con una pizca de sal durante 12–15 minutos hasta que absorba el agua; reserva tapado.",
                "Salpimienta la pechuga de pollo por ambos lados y agrega ajo en polvo.",
                "Calienta una sartén antiadherente a fuego medio-alto con el aceite de oliva y asa el pollo 4–5 minutos por lado hasta dorar y cocinar por dentro.",
                "Cocina el brócoli al vapor o en la misma sartén con 2 cdas de agua tapado por 3 minutos para que quede tierno y verde brillante.",
                "Sirve el arroz como base, acompaña con el brócoli y el pollo cortado en tiras."
            ),
            tips = "Deja reposar el pollo 3 minutos antes de cortarlo para que conserve todos sus jugos.",
            kcal = 495,
            protein = 44,
            carbs = 52,
            fat = 11
        ),
        RecipeOption(
            id = "basic_avena_yogurt_cereales",
            title = "Bowl rápido de avena, yogurt y cereales",
            difficulty = RecipeDifficulty.BASIC,
            prepMinutes = 8,
            matchTags = listOf("avena", "cereales", "yogurt"),
            ingredients = listOf(
                "50 g de avena en hojuelas",
                "150 g de yogurt griego natural",
                "25 g de cereales crujientes o granola ligera",
                "120 ml de agua o leche, canela al gusto"
            ),
            steps = listOf(
                "Coloca la avena en un tazón apto para microondas o cacerola con el líquido y una pizca de canela.",
                "Calienta 2 minutos (moviendo a la mitad) hasta que espese suavemente y deja templar.",
                "Incorpora el yogurt griego por encima para aportar cremosidad y proteína.",
                "Corona justo antes de comer con los cereales crujientes para mantener la textura."
            ),
            tips = "Si la prefieres fría, mezcla avena, leche y yogurt la noche anterior y añade los cereales al despertar.",
            kcal = 390,
            protein = 22,
            carbs = 54,
            fat = 9
        ),
        RecipeOption(
            id = "basic_tostadas_huevo_aguacate",
            title = "Pan tostado con huevo revuelto y aguacate",
            difficulty = RecipeDifficulty.BASIC,
            prepMinutes = 10,
            matchTags = listOf("pan", "huevo", "aguacate", "tomate"),
            ingredients = listOf(
                "2 rebanadas de pan integral o de masa madre",
                "2 huevos enteros + 2 claras",
                "60 g de aguacate machacado",
                "1 tomate pequeño picado, sal y pimienta"
            ),
            steps = listOf(
                "Tuesta las rebanadas de pan hasta que estén doradas y crujientes.",
                "Bate los huevos con las claras y una pizca de sal.",
                "Cocina a fuego bajo en sartén antiadherente moviendo con espátula suavemente para lograr huevos revueltos cremosos.",
                "Unta el aguacate sobre el pan tostado, coloca encima los huevos y termina con tomate picado."
            ),
            tips = "Retira la sartén del fuego unos segundos antes de que el huevo se vea totalmente seco; el calor residual termina la cocción.",
            kcal = 430,
            protein = 26,
            carbs = 34,
            fat = 21
        ),
        RecipeOption(
            id = "basic_penne_atun_tomate",
            title = "Penne rápido con atún y salsa de tomate",
            difficulty = RecipeDifficulty.BASIC,
            prepMinutes = 15,
            matchTags = listOf("penne", "atun", "tomate", "cebolla"),
            ingredients = listOf(
                "85 g de pasta penne (en seco)",
                "1 lata de atún en agua escurrido (120 g)",
                "150 g de tomate triturado o picado",
                "1/4 de cebolla picada, orégano y 1 cdta de aceite de oliva"
            ),
            steps = listOf(
                "Hierve agua con sal y cuece el penne de 9 a 11 minutos hasta que quede al dente; escurre guardando un chorrito del agua de cocción.",
                "Sofríe la cebolla en una sartén con el aceite durante 3 minutos.",
                "Agrega el tomate triturado, orégano y sal; cocina 4 minutos hasta reducir ligeramente.",
                "Mezcla el atún escurrido con el penne y la salsa de tomate, añadiendo 2 cdas del agua de cocción para integrar."
            ),
            tips = "El agua de la pasta contiene almidón que ayuda a que la salsa abrace mejor el penne sin necesidad de crema.",
            kcal = 465,
            protein = 35,
            carbs = 62,
            fat = 8
        ),
        RecipeOption(
            id = "basic_tacos_res_tortilla",
            title = "Tacos sencillos de res asada con cebolla",
            difficulty = RecipeDifficulty.BASIC,
            prepMinutes = 15,
            matchTags = listOf("res", "tortilla", "cebolla", "aguacate", "tomate"),
            ingredients = listOf(
                "150 g de bistec o fajita de res en tiras",
                "3 tortillas de maíz",
                "1/2 cebolla en plumas y 1 tomate picado",
                "50 g de aguacate, limón, sal y comino"
            ),
            steps = listOf(
                "Sazona las tiras de res con sal, pimienta y una pizca de comino.",
                "Saltea la cebolla en sartén caliente 2 minutos, añade la res y cocina 4–5 minutos a fuego alto.",
                "Calienta las tortillas en comal o sartén 30 segundos por lado.",
                "Arma los tacos con la carne, tomate picado, aguacate en láminas y unas gotas de limón."
            ),
            tips = "Usa fuego alto para sellar la res rápidamente y evitar que suelte demasiado líquido.",
            kcal = 480,
            protein = 36,
            carbs = 38,
            fat = 19
        ),
        RecipeOption(
            id = "basic_cerdo_papa_sarten",
            title = "Lomo de cerdo a la sartén con papas doradas",
            difficulty = RecipeDifficulty.BASIC,
            prepMinutes = 22,
            matchTags = listOf("cerdo", "papa", "cebolla"),
            ingredients = listOf(
                "150 g de lomo o chuleta magra de cerdo",
                "200 g de papa en cubos pequeños",
                "1/4 de cebolla picada",
                "1 cdta de aceite de oliva, pimentón dulce y sal"
            ),
            steps = listOf(
                "Cuece los cubos de papa 6 minutos en agua o microondas hasta ablandar ligeramente y escurre.",
                "Dora las papas y la cebolla en sartén con la mitad del aceite y pimentón hasta que queden crujientes por fuera.",
                "Aparta las papas, sella el lomo de cerdo 4 minutos por lado hasta alcanzar cocción completa.",
                "Sirve el cerdo fileteado junto con las papas doradas."
            ),
            tips = "Precocer la papa unos minutos reduce a la mitad el tiempo de sartén y evita que se queme por fuera.",
            kcal = 470,
            protein = 35,
            carbs = 44,
            fat = 16
        ),
        RecipeOption(
            id = "basic_arroz_huevo_espinaca",
            title = "Arroz salteado express con huevo y espinaca",
            difficulty = RecipeDifficulty.BASIC,
            prepMinutes = 12,
            matchTags = listOf("arroz", "huevo", "espinaca", "cebolla"),
            ingredients = listOf(
                "160 g de arroz blanco o integral ya cocido",
                "2 huevos batidos",
                "1 taza grande de espinaca fresca",
                "1/4 de cebolla picada y 1 cda de salsa de soya o sal"
            ),
            steps = listOf(
                "Saltea la cebolla en una sartén caliente con un rocío de aceite durante 2 minutos.",
                "Empuja la cebolla a un lado, agrega los huevos batidos y revuelve suavemente hasta cuajar.",
                "Incorpora el arroz cocido y las espinacas, mezclando a fuego medio-alto 3 minutos.",
                "Sazona con salsa de soya o sal y sirve caliente."
            ),
            tips = "El arroz frío de un día antes queda más suelto y con mejor textura al saltear.",
            kcal = 415,
            protein = 19,
            carbs = 53,
            fat = 13
        ),

        // === INTERMEDIO ===
        RecipeOption(
            id = "inter_fettuccini_pollo_espinaca",
            title = "Fettuccini cremoso ligero con pollo y espinaca",
            difficulty = RecipeDifficulty.INTERMEDIATE,
            prepMinutes = 25,
            matchTags = listOf("fettuccini", "fettucini", "pollo", "espinaca", "queso", "cebolla"),
            ingredients = listOf(
                "85 g de pasta fettuccini",
                "150 g de pechuga de pollo en tiras",
                "60 g de espinaca fresca",
                "60 g de yogurt griego natural o queso crema ligero + 20 g de queso rallado",
                "1 diente de ajo, 1/4 cebolla y nuez moscada"
            ),
            steps = listOf(
                "Cuece el fettuccini en agua con sal hasta quedar al dente y reserva 1/3 de taza del agua de cocción.",
                "Dora las tiras de pollo salpimentadas en sartén amplia durante 5 minutos; retira en un plato.",
                "En la misma sartén, sofríe cebolla y ajo 2 minutos; añade la espinaca hasta que reduzca.",
                "Baja el fuego al mínimo, agrega el yogurt griego (o queso crema), el queso rallado, un poco del agua caliente de pasta y mezcla hasta formar una salsa sedosa.",
                "Regresa el pollo y el fettuccini, integra con movimientos envolventes y termina con pimienta y nuez moscada."
            ),
            tips = "Nunca hiervas la salsa con yogurt a fuego alto para evitar que se corte; usa el calor residual y agua tibia de pasta.",
            kcal = 560,
            protein = 47,
            carbs = 60,
            fat = 14
        ),
        RecipeOption(
            id = "inter_bowl_res_arroz_salteado",
            title = "Bowl salteado de res estilo teriyaki con arroz y brócoli",
            difficulty = RecipeDifficulty.INTERMEDIATE,
            prepMinutes = 25,
            matchTags = listOf("res", "arroz", "brocoli", "cebolla"),
            ingredients = listOf(
                "160 g de carne de res magra en tiras finas",
                "150 g de arroz cocido",
                "120 g de brócoli y 1/3 cebolla",
                "1.5 cdas de salsa de soya, 1 cdta de miel, jengibre o ajo rallado y 1 cdta de maicena"
            ),
            steps = listOf(
                "Mezcla la soya, la miel, 2 cdas de agua, el ajo/jengibre y la maicena en un vaso hasta disolver.",
                "Saltea el brócoli en sartén bien caliente con 1 cdta de aceite durante 3 minutos; retira.",
                "Sella la res en tiras en una sola capa 2–3 minutos sin mover mucho para que dore bien, agrega la cebolla.",
                "Vierte la salsa preparada y el brócoli; remueve 1–2 minutos hasta que espese y glasee la carne.",
                "Sirve sobre el arroz caliente."
            ),
            tips = "Corta la res en contra de la fibra muscular en tiras muy finas para que quede suave al bocado.",
            kcal = 545,
            protein = 41,
            carbs = 58,
            fat = 15
        ),
        RecipeOption(
            id = "inter_penne_pollo_champinones",
            title = "Penne rústico con pollo, champiñones y tomate",
            difficulty = RecipeDifficulty.INTERMEDIATE,
            prepMinutes = 25,
            matchTags = listOf("penne", "pollo", "champinones", "tomate", "queso", "cebolla"),
            ingredients = listOf(
                "85 g de pasta penne",
                "140 g de pollo en cubos",
                "100 g de champiñones laminados",
                "120 g de tomate triturado, 1/4 cebolla, 20 g de queso parmesano o mozzarella"
            ),
            steps = listOf(
                "Cuece el penne al dente y reserva 1/4 taza de agua de cocción.",
                "Dora los champiñones en sartén seca o con rocío de aceite hasta que suelten su jugo y tomen color; aparta.",
                "Sofríe cebolla y dora el pollo salpimentado 5 minutos.",
                "Añade el tomate triturado y cocina 4 minutos; incorpora los champiñones, el penne y un chorrito de agua de cocción.",
                "Sirve con el queso rallado por encima y hojas de albahaca o perejil."
            ),
            tips = "No sales los champiñones al inicio; sálalos al final para que doren en lugar de hervirse.",
            kcal = 535,
            protein = 44,
            carbs = 61,
            fat = 12
        ),
        RecipeOption(
            id = "inter_cerdo_arroz_frijoles",
            title = "Cerdo al pimentón con arroz, frijoles y aguacate",
            difficulty = RecipeDifficulty.INTERMEDIATE,
            prepMinutes = 28,
            matchTags = listOf("cerdo", "arroz", "frijoles", "aguacate", "tomate", "cebolla"),
            ingredients = listOf(
                "150 g de lomo o solomillo de cerdo en cubos",
                "120 g de arroz cocido + 90 g de frijoles cocidos",
                "1 tomate picado, 1/4 cebolla y 50 g de aguacate",
                "Pimentón, comino, ajo en polvo y jugo de 1/2 limón"
            ),
            steps = listOf(
                "Marina el cerdo 10 minutos con pimentón, comino, ajo, sal y unas gotas de limón.",
                "Dora el cerdo en sartén caliente con la cebolla durante 6–7 minutos hasta sellar bien.",
                "Calienta el arroz y los frijoles (puedes saltearlos 2 minutos con el tomate picado).",
                "Arma un bowl con arroz, frijoles, el cerdo especiado y rebanadas de aguacate fresco."
            ),
            tips = "La combinación de arroz + frijoles aporta fibra y carbohidratos de absorción lenta junto a la proteína del cerdo.",
            kcal = 580,
            protein = 40,
            carbs = 62,
            fat = 18
        ),
        RecipeOption(
            id = "inter_espagueti_res_bolonesa",
            title = "Espagueti a la boloñesa casera ligera de res",
            difficulty = RecipeDifficulty.INTERMEDIATE,
            prepMinutes = 30,
            matchTags = listOf("espagueti", "res", "tomate", "cebolla", "queso"),
            ingredients = listOf(
                "85 g de espagueti",
                "150 g de carne molida magra de res (90/10)",
                "180 g de tomate natural triturado",
                "1/3 cebolla finamente picada, 1 diente de ajo, orégano y 15 g de queso"
            ),
            steps = listOf(
                "Cuece el espagueti en abundante agua con sal hasta estar al dente.",
                "Sofríe cebolla y ajo 3 minutos; agrega la carne molida de res y cocina desmenuzando hasta dorar.",
                "Incorpora el tomate triturado, orégano, albahaca y sal; cocina a fuego lento 10 minutos para concentrar sabores.",
                "Mezcla el espagueti con la salsa boloñesa y termina con el queso por encima."
            ),
            tips = "Añade una pizca de zanahoria rallada al sofrito para equilibrar naturalmente la acidez del tomate.",
            kcal = 550,
            protein = 42,
            carbs = 63,
            fat = 14
        ),
        RecipeOption(
            id = "inter_sandwich_pavo_pan_queso",
            title = "Panini dorado de pavo, espinaca, queso y aguacate",
            difficulty = RecipeDifficulty.INTERMEDIATE,
            prepMinutes = 15,
            matchTags = listOf("pan", "pavo", "espinaca", "queso", "aguacate", "tomate"),
            ingredients = listOf(
                "2 rebanadas grandes de pan artesanal o integral",
                "130 g de pechuga de pavo a la plancha o en láminas",
                "30 g de queso mozzarella o panela",
                "Un puñado de espinaca, rodajas de tomate y 40 g de aguacate"
            ),
            steps = listOf(
                "Si usas filete de pavo fresco, márcalo 3 minutos por lado en la sartén con pimienta y sal.",
                "Coloca sobre una rebanada de pan el queso, el pavo, la espinaca y el tomate.",
                "Cierra el panini y tuéstalo en sartén o sandwichera 3 minutos por lado presionando suavemente hasta fundir el queso.",
                "Abre ligeramente al retirar del fuego para añadir láminas de aguacate fresco."
            ),
            tips = "Colocar el queso en contacto con el pan actúa como barrera térmica para que el pan no se humedezca.",
            kcal = 490,
            protein = 38,
            carbs = 40,
            fat = 19
        ),
        RecipeOption(
            id = "inter_pescado_arroz_limon",
            title = "Filete de pescado al ajillo con arroz y espinacas",
            difficulty = RecipeDifficulty.INTERMEDIATE,
            prepMinutes = 22,
            matchTags = listOf("pescado", "arroz", "espinaca", "cebolla", "tomate"),
            ingredients = listOf(
                "170 g de filete de pescado blanco o salmón",
                "150 g de arroz cocido",
                "80 g de espinaca fresca y 6 tomates cherry",
                "2 dientes de ajo laminados, 1 cdta de aceite de oliva y limón"
            ),
            steps = listOf(
                "Seca muy bien el filete con papel absorbente y salpimienta.",
                "Dora suavemente el ajo laminado en el aceite; retira el ajo para que no amargue.",
                "Coloca el pescado en la sartén 3–4 minutos por lado hasta que se desmenuce fácil con tenedor.",
                "En el último minuto agrega las espinacas y tomates partidos a la mitad junto con el jugo de medio limón.",
                "Sirve acompañado del arroz y el ajo dorado por encima."
            ),
            tips = "Secar el pescado antes de llevarlo a la sartén garantiza un sellado dorado sin que se pegue.",
            kcal = 485,
            protein = 39,
            carbs = 49,
            fat = 14
        ),
        RecipeOption(
            id = "inter_fusilli_pollo_pesto_yogurt",
            title = "Fusilli tibio con pollo, brócoli y salsa verde de yogurt",
            difficulty = RecipeDifficulty.INTERMEDIATE,
            prepMinutes = 24,
            matchTags = listOf("fusilli", "pollo", "brocoli", "espinaca", "yogurt", "queso"),
            ingredients = listOf(
                "85 g de pasta fusilli",
                "140 g de pechuga de pollo en cubos",
                "100 g de brócoli en trocitos",
                "50 g de yogurt griego, 30 g de espinaca fresca, 15 g de queso y ajo"
            ),
            steps = listOf(
                "Cuece el fusilli en agua con sal; en los últimos 3 minutos de cocción agrega el brócoli a la misma olla y escurre todo junto.",
                "Licúa la espinaca con el yogurt griego, queso, ajo, sal y 2 cdas de agua para obtener una salsa verde ligera.",
                "Saltea los cubos de pollo en sartén caliente hasta dorar.",
                "Mezcla la pasta y el brócoli tibios con el pollo y la salsa verde fuera del fuego."
            ),
            tips = "Cocer el brócoli en el agua de la pasta los últimos minutos ahorra tiempo y conserva el color verde.",
            kcal = 530,
            protein = 46,
            carbs = 58,
            fat = 12
        ),

        // === AVANZADO ===
        RecipeOption(
            id = "adv_fettuccini_res_champinones",
            title = "Fettuccini al sartén con res sellada, champiñones y reducción",
            difficulty = RecipeDifficulty.ADVANCED,
            prepMinutes = 38,
            matchTags = listOf("fettuccini", "fettucini", "res", "champinones", "cebolla", "queso"),
            ingredients = listOf(
                "90 g de fettuccini",
                "160 g de corte magro de res (sirloin o filete) en medallones o tiras",
                "120 g de champiñones portobello o cremini laminados",
                "1/3 cebolla morada o chalota, 120 ml de caldo de res, tomillo, 20 g de queso parmesano y 1 cdta de mantequilla"
            ),
            steps = listOf(
                "Sazona la res con sal y pimienta 15 minutos antes a temperatura ambiente.",
                "Sella la res en sartén bien caliente 2 minutos por lado para lograr costra Maillard; retira y reserva tapada en reposo.",
                "Baja a fuego medio y saltea la cebolla y champiñones hasta dorar profundamente; desglasa el fondo con el caldo de res y tomillo reduciendo a la mitad.",
                "Cuece el fettuccini al dente y pásalo directo a la sartén con la reducción, añade la mantequilla fría y el parmesano emulsionando con 3 cdas de agua de pasta.",
                "Corta la res en láminas al sesgo y colócala sobre el fettuccini brillante con sus jugos de reposo."
            ),
            tips = "Emulsionar la mantequilla fría con el agua almidonada de la pasta crea una salsa brillante sin crema pesada.",
            kcal = 620,
            protein = 48,
            carbs = 62,
            fat = 19
        ),
        RecipeOption(
            id = "adv_risotto_falso_pollo_champinones",
            title = "Arroz meloso estilo risotto con pollo confitado y champiñones",
            difficulty = RecipeDifficulty.ADVANCED,
            prepMinutes = 40,
            matchTags = listOf("arroz", "pollo", "champinones", "cebolla", "queso"),
            ingredients = listOf(
                "75 g de arroz de grano corto (o arborio/blanco)",
                "150 g de pechuga o muslo sin piel de pollo en cubos",
                "100 g de champiñones, 1/4 cebolla picada muy fina",
                "400 ml de caldo caliente, 25 g de queso parmesano, 1 cdta de aceite de oliva y pimienta negra"
            ),
            steps = listOf(
                "Calienta el caldo en una olla aparte para mantenerlo humeante durante toda la cocción.",
                "Dora el pollo salpimentado en olla ancha; retira. En la misma olla saltea cebolla a fuego bajo hasta transparentar y dora los champiñones.",
                "Agrega el arroz crudo y nacara (tuesta ligeramente) 2 minutos moviendo sin parar hasta que los bordes se vean translúcidos.",
                "Añade el caldo caliente cucharón por cucharón, esperando a que casi se absorba antes de añadir el siguiente y removiendo suavemente durante 18 minutos.",
                "Retira del fuego cuando el arroz esté cremoso y al dente; manteca con el parmesano, incorpora el pollo y reposa 2 minutos."
            ),
            tips = "El secreto de la cremosidad está en añadir el caldo siempre caliente poco a poco para liberar el almidón gradualmente.",
            kcal = 570,
            protein = 43,
            carbs = 64,
            fat = 15
        ),
        RecipeOption(
            id = "adv_cerdo_glaseado_pure_rustico",
            title = "Medallones de cerdo glaseados con puré rústico de papa y espinaca",
            difficulty = RecipeDifficulty.ADVANCED,
            prepMinutes = 42,
            matchTags = listOf("cerdo", "papa", "espinaca", "cebolla", "yogurt"),
            ingredients = listOf(
                "170 g de solomillo de cerdo en medallones gruesos",
                "220 g de papa",
                "60 g de espinaca fresca y 1/4 cebolla",
                "1 cda de mostaza dijon, 1 cdta de miel, 100 ml de caldo y 40 g de yogurt griego para el puré"
            ),
            steps = listOf(
                "Cuece las papas peladas en agua con sal 15–18 minutos; escurre y seca al vapor en la olla 1 minuto antes de machacar con yogurt griego, sal y nuez moscada.",
                "Sella los medallones de cerdo 3 minutos por lado a fuego medio-alto; retira.",
                "En la sartén sofríe cebolla fina, desglasa con caldo, mostaza y miel; reduce hasta obtener un glaseado espeso y brillante.",
                "Regresa el cerdo 1 minuto para impregnarlo con el glaseado y saltea aparte la espinaca con ajo.",
                "Sirve el puré rústico en espejo, la espinaca y los medallones bañados en la reducción."
            ),
            tips = "Secar las papas un minuto en la olla caliente tras escurrir evita un puré aguado y mejora la textura.",
            kcal = 560,
            protein = 42,
            carbs = 56,
            fat = 17
        ),
        RecipeOption(
            id = "adv_penne_alla_vodka_ligero_pollo",
            title = "Penne a la salsa cremosa de tomate rostizado y pollo",
            difficulty = RecipeDifficulty.ADVANCED,
            prepMinutes = 35,
            matchTags = listOf("penne", "pollo", "tomate", "cebolla", "queso", "yogurt"),
            ingredients = listOf(
                "85 g de pasta penne",
                "150 g de pollo en tiras",
                "200 g de tomate maduro o cherry, 1/3 cebolla y 2 dientes de ajo",
                "50 g de yogurt griego o queso ricotta, 15 g de parmesano y hojuelas de chile"
            ),
            steps = listOf(
                "Asa en sartén o comal caliente los tomates, la cebolla y los ajos sin pelar hasta tatemar ligeramente la piel (8 minutos); pela los ajos y tritura todo hasta obtener una salsa suave.",
                "Cocina esa salsa en sartén 6 minutos con chile y sal hasta caramelizar ligeramente los azúcares del tomate.",
                "Sella el pollo aparte con orégano y sal.",
                "Cuece el penne al dente y pásalo a la salsa; fuera del fuego mezcla el yogurt/ricotta y el parmesano con un poco de agua de pasta.",
                "Corona con el pollo sellado y albahaca fresca."
            ),
            tips = "Tatemar o rostizar el tomate antes de licuarlo multiplica el sabor umami sin añadir grasas extras.",
            kcal = 555,
            protein = 45,
            carbs = 63,
            fat = 13
        ),
        RecipeOption(
            id = "adv_salmon_costra_avena_papa",
            title = "Salmón en costra crujiente de avena y hierbas con papas y brócoli",
            difficulty = RecipeDifficulty.ADVANCED,
            prepMinutes = 35,
            matchTags = listOf("pescado", "avena", "papa", "brocoli", "queso"),
            ingredients = listOf(
                "160 g de filete de salmón o pescado firme",
                "30 g de avena molida o en hojuelas finas + 10 g de queso rallado",
                "180 g de papa en gajos + 120 g de brócoli",
                "1 cdta de mostaza, ralladura de limón, perejil y 1 cdta de aceite de oliva"
            ),
            steps = listOf(
                "Precalienta horno o freidora de aire a 200 °C. Mezcla los gajos de papa con sal, pimienta y medio cdta de aceite y hornea 15 minutos.",
                "Mezcla la avena con el queso, perejil picado, ralladura de limón y una pizca de sal.",
                "Unta una capa fina de mostaza sobre la parte superior del pescado y presiona la mezcla de avena formando una costra adherida.",
                "Coloca el pescado y el brócoli junto a las papas y hornea 10–12 minutos más hasta que la costra dore y el pescado esté jugoso."
            ),
            tips = "La mostaza actúa como pegamento aromático ideal para fijar la costra de avena sin usar huevo ni empanizado frito.",
            kcal = 590,
            protein = 41,
            carbs = 49,
            fat = 24
        ),
        RecipeOption(
            id = "adv_espagueti_carbonara_fit_pavo",
            title = "Espagueti a la carbonara auténtica ligera con pavo y huevo",
            difficulty = RecipeDifficulty.ADVANCED,
            prepMinutes = 25,
            matchTags = listOf("espagueti", "pavo", "huevo", "queso"),
            ingredients = listOf(
                "90 g de espagueti",
                "120 g de pechuga o tocino magro de pavo en cubitos",
                "1 huevo entero + 2 yemas",
                "30 g de queso parmesano o pecorino finamente rallado y abundante pimienta negra recién molida"
            ),
            steps = listOf(
                "En un tazón bate el huevo, las yemas, el parmesano rallado y abundante pimienta negra hasta lograr una pasta espesa.",
                "Dora los cubitos de pavo en sartén antiadherente a fuego medio hasta que queden crujientes; apaga el fuego.",
                "Cuece el espagueti al dente y transfiérelo con pinzas directamente a la sartén tibia (sin fuego!) junto con 3 cdas de agua de cocción.",
                "Vierte la mezcla de huevo y queso moviendo vigorosamente 30–40 segundos; el calor residual de la pasta pasteuriza y emulsiona una crema sedosa sin cuajar el huevo."
            ),
            tips = "El truco clave: la sartén debe estar fuera del fuego antes de añadir el huevo para obtener crema y no huevo revuelto.",
            kcal = 565,
            protein = 40,
            carbs = 63,
            fat = 16
        ),
        RecipeOption(
            id = "adv_pan_brioche_avena_pollo_grill",
            title = "Hamburguesa gourmet casera de res en pan artesanal con papas rústicas",
            difficulty = RecipeDifficulty.ADVANCED,
            prepMinutes = 35,
            matchTags = listOf("res", "pan", "papa", "cebolla", "queso", "tomate", "aguacate"),
            ingredients = listOf(
                "150 g de carne molida magra de res",
                "1 bollo o pan integral artesanal",
                "160 g de papa en bastones horneados",
                "20 g de queso, cebolla caramelizada en su jugo, tomate y 40 g de aguacate"
            ),
            steps = listOf(
                "Hornea o asa en freidora de aire los bastones de papa 22 minutos a 200 °C con pimentón y sal.",
                "Carameliza la cebolla a fuego lento en sartén tapada durante 12 minutos añadiendo cucharaditas de agua poco a poco hasta dorar sin azúcar.",
                "Forma un disco con la res sin aplastarlo de más, haz una ligera hendidura en el centro y sella 3–4 minutos por lado fundiendo el queso al final.",
                "Tuesta el interior del pan, arma con tomate, carne, cebolla caramelizada y aguacate, y sirve con las papas."
            ),
            tips = "Hacer una hendidura en el centro de la carne evita que se abombe al cocinarse y mantiene cocción pareja.",
            kcal = 635,
            protein = 44,
            carbs = 58,
            fat = 24
        )
    )

    fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase().trim(), Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    fun findMatchingRecipes(
        selectedIngredients: List<String>,
        difficultyFilter: RecipeDifficulty?
    ): List<RecipeMatchResult> {
        val normalizedSelected = selectedIngredients
            .map { normalize(it) }
            .filter { it.isNotBlank() }
            .distinct()

        val candidates = recipes.filter { recipe ->
            difficultyFilter == null || recipe.difficulty == difficultyFilter
        }

        if (normalizedSelected.isEmpty()) {
            return candidates.map { RecipeMatchResult(it, emptyList()) }
        }

        val scored = candidates.mapNotNull { recipe ->
            val matched = selectedIngredients.filter { userIng ->
                val normIng = normalize(userIng)
                recipe.matchTags.any { tag ->
                    tag.contains(normIng) || normIng.contains(tag)
                } || recipe.ingredients.any { line ->
                    normalize(line).contains(normIng)
                } || normalize(recipe.title).contains(normIng)
            }
            if (matched.isNotEmpty()) {
                RecipeMatchResult(recipe = recipe, matchedIngredients = matched.distinct())
            } else {
                null
            }
        }.sortedWith(
            compareByDescending<RecipeMatchResult> { it.matchedIngredients.size }
                .thenBy { it.recipe.difficulty.ordinal }
                .thenBy { it.recipe.prepMinutes }
        )

        return if (scored.isNotEmpty()) {
            scored
        } else {
            candidates.take(6).map { RecipeMatchResult(it, emptyList()) }
        }
    }
}
