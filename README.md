## Калькулятор расчета поправки по лизинговым депозитам

Для запуска в Windows Вам нужно установить следующие приложения:
* [Maven](https://maven.apache.org/download.cgi)
* [Docker Compose](https://docs.docker.com/compose/install/)
* [JDK 14](https://adoptopenjdk.net/)
***
Запуск приложения:
1. После установки приложений необходимо в папке `/dist` запустить в следующем порядке скрипты:  
    1. `make.bat`   -> компиляция и сборка проекта
    2. `run.bat`   -> запуск проекта
    3. Войдите в браузер и зайдите по адресу http://localhost:8000. Ниже в справке описаны уже имеющиеся в системе учетные записи 
2. Для остановки проекта запустите `stop.bat` (для продолжения запустите `resume.bat`)
3. Для очистки остановленного проекта от ранее созданных файлов запустите `clean.bat`
***
Порядок инициализации калькулятора (аутентификация под именем `superadmin`):
1. Автоматическая генерация периодов на странице администратора. Для этого необходимо ввести даты начала и конца.
2. Автоматический импорт валютных курсов Российского рубля к валютам, указанным в справочнике валют.  
Стандартно в системе присутствуют:  

| Валюта | Наименование валюты
|:----------------:|:---------:|
| RUB | Российский рубль |
| USD | Доллар США |

Импорт осуществляется по коду [ParentCode Центробанка РФ](http://www.cbr.ru/scripts/XML_valFull.asp),  
поэтому при потребности в импорте необходимо добавлять этот код
***
Правила пользования:
1. У каждого депозита должна быть дата окончания в первом периоде его существования, 
отсутствие приведёт к ошибке при попытке расчета; в дальнейших периодах необходимо указывать код депозита, а также период,
в котором изменилась дата возврата.
2. У сценариев существует характеристика ScenarioStornoStatus:
    * ADDITION - при выполнении расчетов по данному сценарию имеющиеся проводки за рассчитываемый период сторнируются;
    * FULL - при выполнении расчетов по данному сценарию все имеющиеся проводки сторнируются;
3. Не допускается рассчитывать депозиты со сценария на сценарий:  
    :white_check_mark: ADDITION -> ADDITION ***для одного сценария***;  
    :white_check_mark: ADDITION -> FULL;  
    :negative_squared_cross_mark: ADDITION -> ADDITION ***для разных сценариев***;  
    :negative_squared_cross_mark: FULL -> ADDITION  
    :negative_squared_cross_mark: FULL -> FULL
4. Возможности в работающем функционале:
    * Генерация периодов согласно указанным датам 
    * Импорт курсов валют в автоматическом режиме с сайта Центробанка РФ согласно указанному режиму и существующим периодам в системе.
2. В системе при загрузке создаются такие пользователи: 
> ***Суперпользователь*** имеет следующие параметры:  
```
username: superadmin
password: a
```
> ***Тестовый пользователь-1*** имеет следующие параметры:  
```
username: testuser1
password: a
```
> ***Тестовый пользователь-2*** имеет следующие параметры:  
```
username: testuser2
password: a
```
Для безопасного использования следует из либо удалить, либо скорректировать их параметры    
    
***