create table authorities (
       id bigint not null,
       name varchar(255) not null,
       primary key (id),
       unique (name)
);

create table company (
       id bigint not null auto_increment,
       code varchar(10) not null,
       date_time_last_change datetime(6) not null,
       name varchar(255) not null,
       user_id bigint not null,
       primary key (id)
);

create table counterpartner (
       id bigint not null auto_increment,
       date_time_last_change datetime(6) not null,
       name varchar(255) not null,
       user_id bigint not null,
       primary key (id)
);

create table currency (
       id bigint not null auto_increment,
       cbrcurrency_code varchar(6),
       date_time_last_change datetime(6) not null,
       name varchar(255) not null,
       short_name varchar(3) not null,
       user_id bigint not null,
       primary key (id)
);

create table deposit_rate (
       end_period DATE not null,
       start_period DATE not null,
       rate decimal(19,2) not null,
       date_time_last_change datetime(6) not null,
       currency_id bigint not null,
       company_id bigint not null,
       scenario_id bigint not null,
       duration_id bigint not null,
       user_id bigint not null,

       primary key (end_period,
       start_period,
       company_id,
       currency_id,
       duration_id,
       scenario_id)
);

create table duration (
       id bigint not null auto_increment,
       max_month integer not null,
       min_month integer not null,
       date_time_last_change datetime(6) not null,
       name varchar(255) not null,
       user_id bigint not null,
       primary key (id)
);

create table end_date (
       end_date DATE not null,
       date_time_last_change datetime(6) not null,
       leasing_deposit_id bigint not null,
       scenario_id bigint not null,
       period_id bigint not null,
       user_id bigint not null,
       primary key (leasing_deposit_id,
       period_id,
       scenario_id)
);

create table entries (
       calculation_time datetime(6) not null,
       accum_amort_discont_end_period_rub_reg_ld_2_n DECIMAL(30,10) not null,
       accum_amort_discont_end_period_rub_reg_ld_3_s DECIMAL(30,10) not null,
       accum_amort_discont_end_period_cur_reg_ld_2_j DECIMAL(30,10) not null,
       accum_amort_discont_start_period_rub_reg_ld_2_k DECIMAL(30,10) not null,
       accum_amort_discont_start_period_rub_reg_ld_3_r DECIMAL(30,10) not null,
       accum_amort_discont_start_period_cur_reg_ld_2_h DECIMAL(30,10) not null,
       advance_currentperiod_reg_ld_3_ae DECIMAL(30,10) not null,
       advance_prevperiod_reg_ld_3_af DECIMAL(30,10) not null,
       amort_discont_current_period_rub_reg_ld_2_m DECIMAL(30,10) not null,
       amort_discont_current_period_cur_reg_ld_2_i DECIMAL(30,10) not null,
       corr_acc_amort_disc_rub_reg_ld_1_t DECIMAL(30,10) not null,
       corr_new_date_higher_corr_acc_amort_disc_rub_reg_ld_1_v DECIMAL(30,10) not null,
       corr_new_date_higher_discont_rub_reg_ld_1_u DECIMAL(30,10) not null,
       corr_new_date_less_corr_acc_amort_disc_rub_reg_ld_1_x DECIMAL(30,10) not null,
       corr_new_date_less_discont_rub_reg_ld_1_w DECIMAL(30,10) not null,
       discont_at_start_date_rub_reg_ld_1_l DECIMAL(30,10) not null,
       discont_at_start_date_rub_forifrsacc_reg_ld_1_m DECIMAL(30,10) not null,
       discont_at_start_date_cur_reg_ld_1_k DECIMAL(30,10) not null,
       discont_sum_at_new_end_date_cur_reg_ld_1_p DECIMAL(30,10) not null,
       disc_diff_betw_disconts_rub_reg_ld_1_r DECIMAL(30,10) not null,
       disc_sum_at_new_end_date_rub_reg_ld_1_q DECIMAL(30,10) not null,
       disposal_body_rub_reg_ld_3_x DECIMAL(30,10) not null,
       disposal_discont_rub_reg_ld_3_y DECIMAL(30,10) not null,
       incoming_ld_body_rub_reg_ld_3_l DECIMAL(30,10) not null,
       ldterm_reg_ld_3_z enum('ST','LT') not null,
       outcoming_ld_body_reg_ld_3_m DECIMAL(30,10) not null,
       reval_acc_amort_minus_rub_reg_ld_3_u DECIMAL(30,10) not null,
       reval_acc_amort_plus_rub_reg_ld_3_t DECIMAL(30,10) not null,
       reval_corr_disc_rub_reg_ld_1_s DECIMAL(30,10) not null,
       reval_ld_body_minus_reg_ld_3_o DECIMAL(30,10) not null,
       reval_ld_body_plus_reg_ld_3_n DECIMAL(30,10) not null,
       sum_minus_forex_diff_reg_ld_w DECIMAL(30,10) not null,
       sum_plus_forex_diff_reg_ld_3_v DECIMAL(30,10) not null,
       status_entry_made_during_or_after_closed_period enum('CURRENT_PERIOD', 'AFTER_CLOSING_PERIOD') not null,
       termreclass_body_currentperiod_reg_ld_3_aa DECIMAL(30,10) not null,
       termreclass_body_prevperiod_reg_ld_3_ac DECIMAL(30,10) not null,
       termreclass_percent_currentperiod_reg_ld_3_ab DECIMAL(30,10) not null,
       termreclass_percent_prevperiod_reg_ld_3_ad DECIMAL(30,10) not null,
       deposit_sum_not_disc_rub_reg_ld_1_n DECIMAL(30,10) not null,
       discounted_sum_at_current_end_date_cur_reg_ld_3_g DECIMAL(30,10) not null,
       end_date_at_this_period DATE not null,
       date_time_last_change datetime(6) not null,
       percent_rate_for_period_forld decimal(19,2) not null,
       transaction_status enum('ACTUAL', 'STORNO', 'DELETED'),
       leasing_deposit_id bigint not null,
       scenario_id bigint not null,
       period_id bigint not null,
       user_id bigint not null,

       primary key (calculation_time,
       leasing_deposit_id,
       period_id,
       scenario_id)
);

create table entryifrsacc (
       date_time_last_change datetime(6) not null,
       sum DECIMAL(30,10) not null,
       ifrs_account_id bigint not null,
       entry_calculation_time datetime(6) not null,
       entry_leasing_deposit_id bigint not null,
       entry_period_id bigint not null,
       entry_scenario_id bigint not null,
       user_id bigint not null,

       primary key (entry_calculation_time,
       entry_leasing_deposit_id,
       entry_period_id,
       entry_scenario_id,
       ifrs_account_id)
);

create table exchange_rate (
       date datetime(6) not null,
       average_rate_for_month DECIMAL(31,12),
       date_time_last_change datetime(6) not null,
       rate_at_date DECIMAL(31,12) not null,
       currency_id bigint not null,
       scenario_id bigint not null,
       user_id bigint not null,

       primary key (currency_id,
       date,
       scenario_id)
);

create table hibernate_sequence (next_val bigint);
insert into hibernate_sequence values ( 1 );
insert into hibernate_sequence values ( 1 );
insert into hibernate_sequence values ( 1 );

create table ifrs_account (
       id bigint not null auto_increment,
       account_code varchar(40),
       account_name varchar(255),
       ct varchar(255),
       dr varchar(255),
       flow_code varchar(255),
       flow_name varchar(255),
       is_inverse_sum bit not null,
       date_time_last_change datetime(6) not null,
       mapping_form_and_column varchar(255),
       pa varchar(255),
       sh varchar(255),
       user_id bigint not null,

       primary key (id)
);

create table leasing_deposits (
       id bigint not null auto_increment,
       deposit_sum_not_disc decimal(19,2) not null,
       is_created enum('X'),
       is_deleted enum('X'),
       date_time_last_change datetime(6) not null,
       start_date DATE not null,
       company_id bigint not null,
       counterpartner_id bigint not null,
       currency_id bigint not null,
       scenario_id bigint not null,
       user_id bigint not null,

       primary key (id)
);

create table period (
       id bigint not null auto_increment,
       date datetime(6) not null,
       date_time_last_change datetime(6) not null,
       user_id bigint not null,

       primary key (id),
       unique (date)
);

create table periods_closed (
       isclosed enum('X'),
       date_time_last_change datetime(6) not null,
       scenario_id bigint not null,
       period_id bigint not null,
       user_id bigint not null,

       primary key (period_id,
       scenario_id)
);

create table role_authorities (
       role_id bigint not null,
       authority_id bigint not null,

       primary key (role_id, authority_id)
);

create table roles (
       id bigint not null,
       date_time_last_change datetime(6) not null,
       name varchar(255) not null,
       user_changed_id bigint not null,

       primary key (id),
       unique (name)
);

create table scenario (
       id bigint not null auto_increment,
       is_blocked enum('X'),
       date_time_last_change datetime(6) not null,
       name varchar(255) not null,
       storno_status enum('FULL','ADDITION') not null,
       user_id bigint not null,

       primary key (id)
);

create table users (
       id bigint not null,
       is_account_expired enum('X'),
       is_credentials_expired enum('X'),
       is_enabled enum('X'),
       is_locked enum('X'),
       date_time_last_change datetime(6) not null,
       password varchar(255) not null,
       username varchar(255) not null,
       user_changed_id bigint,

       primary key (id),
       unique (username)
);

create table users_roles (
       user_id bigint not null,
       role_id bigint not null
);

alter table company
    add constraint company_users_fk
    foreign key (user_id) references users (id);

alter table counterpartner
    add constraint counterpartner_users_fk
    foreign key (user_id) references users (id);

alter table currency
    add constraint currency_users_fk
    foreign key (user_id) references users (id);

alter table duration
    add constraint duration_users_fk
    foreign key (user_id) references users (id);

alter table ifrs_account
    add constraint ifrs_account_users_fk
    foreign key (user_id) references users (id);

alter table period
    add constraint period_users_fk
    foreign key (user_id) references users (id);



alter table users_roles
    add constraint users_roles_users_fk
    foreign key (user_id) references users (id);

alter table users_roles
    add constraint users_roles_roles_fk
    foreign key (role_id) references roles (id);

alter table roles
    add constraint roles_users_id_fk
    foreign key (user_changed_id) references users (id);

alter table scenario
    add constraint users_scenario_fk
    foreign key (user_id) references users (id);

alter table users
    add constraint users_user_changed_fk
    foreign key (user_changed_id) references users (id);

alter table deposit_rate
    add constraint deposit_rate_users_fk
    foreign key (user_id) references users (id);

alter table deposit_rate
    add constraint deposit_rate_currency_fk
    foreign key (currency_id) references currency (id);

alter table deposit_rate
    add constraint deposit_rate_company_fk
    foreign key (company_id) references company (id);

alter table deposit_rate
    add constraint deposit_rate_scenario_fk
    foreign key (scenario_id) references scenario (id);

alter table deposit_rate
    add constraint deposit_rate_duration_fk
    foreign key (duration_id) references duration (id);

alter table end_date
    add constraint end_date_users_fk
    foreign key (user_id) references users (id);

alter table end_date
    add constraint end_date_scenario_fk
    foreign key (scenario_id) references scenario (id);

alter table end_date
    add constraint end_date_period_fk
    foreign key (period_id) references period (id);

alter table end_date
    add constraint end_date_leasing_deposit_fk
    foreign key (leasing_deposit_id) references leasing_deposits (id);

alter table entries
    add constraint entries_users_fk
    foreign key (user_id) references users (id);

alter table entries
    add constraint entries_scenario_fk
    foreign key (scenario_id) references scenario (id);

alter table entries
    add constraint entries_period_fk
    foreign key (period_id) references period (id);

alter table entries
    add constraint entries_leasing_deposits_fk
    foreign key (leasing_deposit_id) references leasing_deposits (id);

alter table entryifrsacc
    add constraint entryifrsacc_ifrs_account_fk
    foreign key (ifrs_account_id) references ifrs_account (id);

alter table entryifrsacc
    add constraint entryifrsacc_entry_fk
    foreign key (entry_calculation_time,
       entry_leasing_deposit_id,
       entry_period_id,
       entry_scenario_id) references entries (calculation_time,
       leasing_deposit_id,
       period_id,
       scenario_id);

alter table entryifrsacc
    add constraint entryifrsacc_users_fk
    foreign key (user_id) references users (id);

alter table exchange_rate
    add constraint exchange_rate_users
    foreign key (user_id) references users (id);

alter table exchange_rate
    add constraint exchange_rate_currency
    foreign key (currency_id) references currency (id);

alter table exchange_rate
    add constraint exchange_rate_scenario
    foreign key (scenario_id) references scenario (id);

alter table leasing_deposits
    add constraint leasing_deposits_users
    foreign key (user_id) references users (id);

alter table leasing_deposits
    add constraint leasing_deposits_companies
    foreign key (company_id) references company (id);

alter table leasing_deposits
    add constraint leasing_deposits_counterpartners
    foreign key (counterpartner_id) references counterpartner (id);

alter table leasing_deposits
    add constraint leasing_deposits_currencies
    foreign key (currency_id) references currency (id);

alter table leasing_deposits
    add constraint leasing_deposits_scenarios
    foreign key (scenario_id) references scenario (id);

alter table periods_closed
    add constraint periods_closed_users
    foreign key (user_id) references users (id);

alter table periods_closed
    add constraint periods_closed_scenarios
    foreign key (scenario_id) references scenario (id);

alter table periods_closed
    add constraint periods_closed_period
    foreign key (period_id) references period (id);

alter table role_authorities
    add constraint role_authorities_authorities
    foreign key (authority_id) references authorities (id);

alter table role_authorities
    add constraint role_authorities_roles
    foreign key (role_id) references roles (id);

INSERT INTO authorities(id, name) VALUES (1, "USER_ADDER"), (2, "USER_EDITOR"),
	(3, "USER_DELETER"), (4, "USER_READER"), (5, "ROLE_ADDER"), (6, "ROLE_EDITOR"),
	(7, "ROLE_DELETER"), (8, "ROLE_READER"), (9, "COMPANY_ADDER"), (10, "COMPANY_EDITOR"),
	(11, "COMPANY_DELETER"), (68, "COMPANY_READER"), (12, "COUNTERPARTNER_ADDER"),
	(13, "COUNTERPARTNER_EDITOR"), (14, "COUNTERPARTNER_DELETER"), (15, "COUNTERPARTNER_READER"),
	(16, "CURRENCY_ADDER"), (17, "CURRENCY_EDITOR"), (18, "CURRENCY_DELETER"), (19, "CURRENCY_READER"),
	(20, "DEPOSIT_RATES_ADDER"), (21, "DEPOSIT_RATES_EDITOR"), (22, "DEPOSIT_RATES_DELETER"),
	(23, "DEPOSIT_RATES_READER"), (24, "DURATION_ADDER"), (25, "DURATION_EDITOR"),
	(26, "DURATION_DELETER"), (27, "DURATION_READER"), (28, "END_DATE_ADDER"), (29, "END_DATE_EDITOR"),
	(30, "END_DATE_DELETER"), (31, "END_DATE_READER"), (32, "ENTRY_ADDER"), (33, "ENTRY_EDITOR"),
	(34, "ENTRY_DELETER"), (35, "ENTRY_READER"), (36, "ENTRY_IFRS_ADDER"), (37, "ENTRY_IFRS_EDITOR"),
	(38, "ENTRY_IFRS_DELETER"), (39, "ENTRY_IFRS_READER"), (40, "EXCHANGE_RATE_ADDER"),
	(41, "EXCHANGE_RATE_EDITOR"), (42, "EXCHANGE_RATE_DELETER"), (43, "EXCHANGE_RATE_READER"),
	(44, "IFRS_ACC_ADDER"), (45, "IFRS_ACC_EDITOR"), (46, "IFRS_ACC_DELETER"), (47, "IFRS_ACC_READER"),
	(48, "LEASING_DEPOSIT_ADDER"), (49, "LEASING_DEPOSIT_EDITOR"), (50, "LEASING_DEPOSIT_DELETER"),
	(51, "LEASING_DEPOSIT_READER"), (52, "PERIOD_ADDER"), (53, "PERIOD_EDITOR"), (54, "PERIOD_DELETER"),
	(55, "PERIOD_READER"), (56, "PERIODS_CLOSED_ADDER"), (57, "PERIODS_CLOSED_EDITOR"),
	(58, "PERIODS_CLOSED_DELETER"), (59, "PERIODS_CLOSED_READER"), (60, "SCENARIO_ADDER"),
	(61, "SCENARIO_EDITOR"), (62, "SCENARIO_DELETER"), (63, "SCENARIO_READER"), (64, "LOAD_EXCHANGE_RATE_FROM_CBR"),
	(65, "AUTO_ADDING_PERIODS"), (66, "AUTO_CLOSING_PERIODS"), (67, "CALCULATE");

INSERT INTO users(id, username, password, user_changed_id,
    date_time_last_change, is_locked, is_enabled,
    is_credentials_expired, is_account_expired) VALUES
    (1, "a", "$2a$10$RWxBp/xQm9YOHFsTMSs.suKLFipCu4OszqN7JnK1Z1PQwKB6FacA.", null,
    "2020-05-27 21:05:12.937360",
    null, 'X', null, null);

INSERT INTO roles(id, name, user_changed_id, date_time_last_change) VALUES
(1, "ROLE_SUPERADMIN", 1, "2020-05-27 21:05:12.937360");

INSERT INTO role_authorities(role_id, authority_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9),
(1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15), (1, 16), (1, 17), (1, 18),
(1, 19), (1, 20), (1, 21), (1, 22), (1, 23), (1, 24), (1, 25), (1, 26), (1, 27),
(1, 28), (1, 29), (1, 30), (1, 31), (1, 32), (1, 33), (1, 34), (1, 35), (1, 36),
(1, 37), (1, 38), (1, 39), (1, 40), (1, 41), (1, 42), (1, 43), (1, 44), (1, 45),
(1, 46), (1, 47), (1, 48), (1, 49), (1, 50), (1, 51), (1, 52), (1, 53), (1, 54),
(1, 55), (1, 56), (1, 57), (1, 58), (1, 59), (1, 60), (1, 61), (1, 62), (1, 63),
(1, 64), (1, 65), (1, 66), (1, 67), (1, 68);

INSERT INTO users_roles(user_id, role_id) VALUES (1, 1);

INSERT INTO currency(id, name, short_name, user_id, cbrcurrency_code, date_time_last_change)
VALUES (1, "Russian Ruble", "RUB", 1, null, "2020-05-27 21:05:12.937360"),
(2, "USA Dollar", "USD", 1, "R01235", "2020-05-27 21:05:12.937360");

INSERT INTO scenario(id, name, storno_status, user_id, is_blocked, date_time_last_change)
VALUES (1, "FACT", "ADDITION", 1, null, "2020-05-27 21:05:12.937360");

INSERT INTO counterpartner(id, name, user_id, date_time_last_change)
VALUES (1, "SuperBP", 1, "2020-05-27 21:05:12.937360");

INSERT INTO company(id, code, name, user_id, date_time_last_change)
VALUES (1, "C1001", "SuperCompany", 1, "2020-05-27 21:05:12.937360");

INSERT INTO ifrs_account(id, account_code, account_name, flow_code, flow_name, dr, pa, ct, sh,
 is_inverse_sum, mapping_form_and_column, date_time_last_change, user_id)
VALUES
(1, "A0203010100", "Долгосрочные депозиты по аренде ВС - основная сумма",
"F2000", "Поступление", "-", "THP99", "RUB", "-", false, "Reg.LD.1=N5", "2020-05-27 21:05:12.937360", 1),
(2, "A0208010000", "Долгосрочные авансы выданные", "F2000", "Поступление",
"-", "THP99", "RUB", "-", true, "Reg.LD.1=N5", "2020-05-27 21:05:12.937360", 1),
(3, "A0208010000", "Долгосрочные авансы выданные", "F2000", "Поступление", "-",
"THP99", "RUB", "-", true, "Reg.LD.1=M5", "2020-05-27 21:05:12.937360", 1),
(4, "A0203010100", "Долгосрочные депозиты по аренде ВС - основная сумма",
"F2000", "Поступление", "-", "THP99", "RUB", "-", false, "Reg.LD.1=M5", "2020-05-27 21:05:12.937360", 1),
(5,                 "P0302990000",
                "Прочие финансовые расходы",
                "Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", true,
                "Reg.LD.1=U5 + Reg.LD.1=V5", "2020-05-27 21:05:12.937360", 1),
(6,
        "A0203010100",
        "Долгосрочные депозиты по аренде ВС - основная сумма",
        "F2700", "Начисление процентных доходов/расходов/дисконта", "-", "THP99", "RUB",
        "-", false, "Reg.LD.1=U5", "2020-05-27 21:05:12.937360", 1),

(7,
        "A0203010200",
        "Долгосрочные депозиты по аренде ВС - проценты",
        "F2700", "Начисление процентных доходов/расходов/дисконта", "-", "THP99", "RUB",
        "-", false, "Reg.LD.1=V5", "2020-05-27 21:05:12.937360", 1),

(8,
        "P0301990000",
        "Прочие финансовые доходы",
        "Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", true,
        "Reg.LD.1=W5 + Reg.LD.1=X5", "2020-05-27 21:05:12.937360", 1),

(9,
        "A0203010100",
        "Долгосрочные депозиты по аренде ВС - основная сумма",
        "F2700", "Начисление процентных доходов/расходов/дисконта", "-", "THP99", "RUB",
        "-", false, "Reg.LD.1=W5", "2020-05-27 21:05:12.937360", 1),

(10,
        "A0203010200",
        "Долгосрочные депозиты по аренде ВС - проценты",
        "F2700", "Начисление процентных доходов/расходов/дисконта", "-", "THP99", "RUB",
        "-", false, "Reg.LD.1=X5", "2020-05-27 21:05:12.937360", 1),

(11,
        "A0203010200",
        "Долгосрочные депозиты по аренде ВС - проценты",
        "F2700", "Начисление процентных доходов/расходов/дисконта", "-", "THP99", "RUB",
        "-", false, "Reg.LD.2=M5", "2020-05-27 21:05:12.937360", 1),

(12,
        "P0301020000",
        "Процентные доходы по страховым депозитам",
        "Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", true,
        "Reg.LD.2=M5", "2020-05-27 21:05:12.937360", 1),

(13,
        "A0203010100",
        "Долгосрочные депозиты по аренде ВС - основная сумма",
        "F1600", "Курсовая разница", "-", "THP99", "RUB", "-", false, "Reg.LD.3=N5", "2020-05-27 21:05:12.937360", 1),

(14,
        "A0203010100",
        "Долгосрочные депозиты по аренде ВС - основная сумма",
        "F1600", "Курсовая разница", "-", "THP99", "RUB", "-", false, "Reg.LD.3=O5", "2020-05-27 21:05:12.937360", 1),

(15,
        "A0203010200",
        "Долгосрочные депозиты по аренде ВС - проценты",
        "F1600", "Курсовая разница", "-", "THP99", "RUB", "-", false, "Reg.LD.3=T5", "2020-05-27 21:05:12.937360", 1),

(16,
        "A0203010200",
        "Долгосрочные депозиты по аренде ВС - проценты",
        "F1600", "Курсовая разница", "-", "THP99", "RUB", "-", false, "Reg.LD.3=U5", "2020-05-27 21:05:12.937360", 1),

(17,
        "P0301310000",
        "Положительные курсовые разницы",
        "Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", false,
        "Reg.LD.3=V5", "2020-05-27 21:05:12.937360", 1),

(18,
        "P0302310000",
        "Отрицательные курсовые разницы",
        "Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", false,
        "Reg.LD.3=W5", "2020-05-27 21:05:12.937360", 1),

(19,
        "A0107010000",
        "Краткосрочные авансы выданные",
        "F3000", "Выбытие", "-", "THP99", "RUB", "-", false, "Reg.LD.3=X5 + Reg.LD.3=Y5", "2020-05-27 21:05:12.937360", 1),

(20,
        "A0203010100",
        "Долгосрочные депозиты по аренде ВС - основная сумма",
        "F3000", "Выбытие", "-", "THP99", "RUB", "-", true, "Reg.LD.3=X5", "2020-05-27 21:05:12.937360", 1),

(21,
        "A0203010200",
        "Долгосрочные депозиты по аренде ВС - проценты",
        "F3000", "Выбытие", "-", "THP99", "RUB", "-", true, "Reg.LD.3=Y5", "2020-05-27 21:05:12.937360", 1),

(22,
        "P0301310000",
        "Положительные курсовые разницы",
        "Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", false,
        "Reg.LD.4_MA_AFL=B1", "2020-05-27 21:05:12.937360", 1),

(23,
        "P0302310000",
        "Отрицательные курсовые разницы",
        "Y9900", "Накопительно с начала года", "-", "THP99", "-", "-", false,
        "Reg.LD.4_MA_AFL=C1", "2020-05-27 21:05:12.937360", 1),

(24,
        "A0107010000",
        "Краткосрочные авансы выданные",
        "F1600", "Курсовая разница", "-", "THP99", "RUB", "-", false,
        "Reg.LD.4_MA_AFL=A1", "2020-05-27 21:05:12.937360", 1),

(25,
        "A0215010100",
        "АПП воздушные суда и авиационные двигатели - ПСт",
        "F2006", "Ввод в эксплуатацию", "-", "THP99", "-", "-", false, "APP-1", "2020-05-27 21:05:12.937360", 1),

(26,
        "A0215020100",
        "АПП земля - ПСт",
        "F2006", "Ввод в эксплуатацию", "-", "THP99", "-", "-", false, "APP-2", "2020-05-27 21:05:12.937360", 1),

(27,
        "A0215030100",
        "АПП здания - ПСт",
        "F2006", "Ввод в эксплуатацию", "-", "THP99", "-", "-", false, "APP-3", "2020-05-27 21:05:12.937360", 1),

(28,
        "A0215040100",
        "АПП машины и оборудование - ПСт",
        "F2006", "Ввод в эксплуатацию", "-", "THP99", "-", "-", false, "APP-4", "2020-05-27 21:05:12.937360", 1),

(29,
        "A0215050100",
        "АПП прочие ОС - ПСт",
        "F2006", "Ввод в эксплуатацию", "-", "THP99", "-", "-", false, "APP-5", "2020-05-27 21:05:12.937360", 1),

(30,
        "A0215060100",
        "АПП незавершенное строительство - первоначальная стоимость",
        "F2006", "Ввод в эксплуатацию", "-", "THP99", "-", "-", false, "APP-6", "2020-05-27 21:05:12.937360", 1),

(31,
        "A0208010000",
        "Долгосрочные авансы выданные",
        "F3000", "Выбытие", "-", "THP99", "RUB", "-", true, "APP-7", "2020-05-27 21:05:12.937360", 1),

(32,
        "A0208010000",
        "Долгосрочные авансы выданные",
        "F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB",
        "-", true, "Reg.LD.3=AE5-Reg.LD.3=AF5", "2020-05-27 21:05:12.937360", 1),

(33,
        "A0107010000",
        "Краткосрочные авансы выданные",
        "F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB",
        "-", false, "Reg.LD.3=AE5-Reg.LD.3=AF5", "2020-05-27 21:05:12.937360", 1),

(34,
        "A0203010100",
        "Долгосрочные депозиты по аренде ВС - основная сумма",
        "F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB",
        "-", true, "Reg.LD.3=AA5-Reg.LD.3=AC5", "2020-05-27 21:05:12.937360", 1),

(35,
        "A0102010100",
        "Краткосрочные депозиты по аренде ВС - основная сумма",
        "F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB",
        "-", false, "Reg.LD.3=AA5-Reg.LD.3=AC5", "2020-05-27 21:05:12.937360", 1),

(36,
        "A0203010200",
        "Долгосрочные депозиты по аренде ВС - проценты",
        "F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB",
        "-", true, "Reg.LD.3=AB5-Reg.LD.3=AD5", "2020-05-27 21:05:12.937360", 1),

(37,
        "A0102010200",
        "Краткосрочные депозиты по аренде ВС - проценты",
        "F5000", "Реклассификация между группами актива/обязательства", "-", "THP99", "RUB",
        "-", false, "Reg.LD.3=AB5-Reg.LD.3=AD5", "2020-05-27 21:05:12.937360", 1);

INSERT INTO duration(id, max_month, min_month, date_time_last_change, name, user_id)
VALUES
(1, 12, 0, "2020-05-27 21:05:12.937360", "<= 12 мес.", 1),
(2, 24, 13, "2020-05-27 21:05:12.937360", "13-24 мес.", 1),
(3, 36, 25, "2020-05-27 21:05:12.937360", "25-36 мес.", 1),
(4, 48, 37, "2020-05-27 21:05:12.937360", "37-48 мес.", 1),
(5, 60, 49, "2020-05-27 21:05:12.937360", "49-60 мес.", 1),
(6, 72, 61, "2020-05-27 21:05:12.937360", "61-72 мес.", 1),
(7, 84, 73, "2020-05-27 21:05:12.937360", "73-84 мес.", 1),
(8, 96, 85, "2020-05-27 21:05:12.937360", "85-96 мес.", 1),
(9, 108, 97, "2020-05-27 21:05:12.937360", "97-108 мес.", 1),
(10, 120, 109, "2020-05-27 21:05:12.937360", "109-120 мес.", 1),
(11, 132, 121, "2020-05-27 21:05:12.937360", "121-132 мес.", 1),
(12, 144, 133, "2020-05-27 21:05:12.937360", "133-144 мес.", 1),
(13, 100000, 145, "2020-05-27 21:05:12.937360", "> 144 мес.", 1);