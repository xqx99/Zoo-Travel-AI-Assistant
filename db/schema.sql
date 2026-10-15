-- ============================================
-- 动物园旅游智能规划助手 数据库初始化脚本
-- MySQL 8.0 / utf8mb4
-- ============================================

CREATE DATABASE IF NOT EXISTS zootravel DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE zootravel;

-- ---------- 动物园主表 ----------
DROP TABLE IF EXISTS zoo_animal;
DROP TABLE IF EXISTS knowledge_doc;
DROP TABLE IF EXISTS animal;
DROP TABLE IF EXISTS zoo;

CREATE TABLE zoo (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  name            VARCHAR(64)  NOT NULL COMMENT '动物园名称',
  city            VARCHAR(32)  NOT NULL COMMENT '城市',
  province        VARCHAR(32)  DEFAULT NULL COMMENT '省份',
  address         VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
  longitude       DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
  latitude        DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  ticket_adult    DECIMAL(8,2) DEFAULT NULL COMMENT '成人票价格(元)',
  ticket_child    DECIMAL(8,2) DEFAULT NULL COMMENT '儿童票价格(元)',
  ticket_note     VARCHAR(255) DEFAULT NULL COMMENT '门票备注',
  open_time       VARCHAR(64)  DEFAULT NULL COMMENT '常规开放时间',
  open_hours_note VARCHAR(255) DEFAULT NULL COMMENT '开放时间备注',
  website         VARCHAR(128) DEFAULT NULL COMMENT '官网',
  description     TEXT COMMENT '简介',
  star_animals    VARCHAR(255) DEFAULT NULL COMMENT '明星动物摘要',
  created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_city (city),
  KEY idx_name (name)
) ENGINE=InnoDB COMMENT='动物园';

-- ---------- 动物字典 ----------
CREATE TABLE animal (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  name            VARCHAR(64) NOT NULL COMMENT '动物名',
  scientific_name VARCHAR(128) DEFAULT NULL COMMENT '学名',
  category        VARCHAR(32)  DEFAULT NULL COMMENT '类别: 哺乳/鸟类/爬行/两栖/海洋',
  habitat         VARCHAR(64)  DEFAULT NULL COMMENT '栖息地',
  diet            VARCHAR(64)  DEFAULT NULL COMMENT '食性',
  description     TEXT COMMENT '简介',
  fun_facts       TEXT COMMENT '冷知识',
  KEY idx_name (name)
) ENGINE=InnoDB COMMENT='动物字典';

-- ---------- 动物园-动物关联 ----------
CREATE TABLE zoo_animal (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  zoo_id       BIGINT NOT NULL COMMENT '动物园ID',
  animal_id    BIGINT NOT NULL COMMENT '动物ID',
  exhibit_area VARCHAR(64)  DEFAULT NULL COMMENT '展区',
  show_time    VARCHAR(128) DEFAULT NULL COMMENT '投喂/表演时间',
  is_star      TINYINT DEFAULT 0 COMMENT '是否明星动物 1=是',
  note         VARCHAR(255) DEFAULT NULL COMMENT '备注',
  UNIQUE KEY uk_zoo_animal (zoo_id, animal_id),
  KEY idx_zoo (zoo_id),
  KEY idx_animal (animal_id)
) ENGINE=InnoDB COMMENT='动物园动物关联';

-- ---------- RAG 知识库源文档 ----------
CREATE TABLE knowledge_doc (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  title      VARCHAR(128) NOT NULL COMMENT '标题',
  category   VARCHAR(32)  DEFAULT NULL COMMENT '分类: 熊猫攻略/Tips/路线/亲子',
  content    MEDIUMTEXT NOT NULL COMMENT '正文(供切分入向量库)',
  doc_source VARCHAR(64)  DEFAULT NULL COMMENT '来源',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='RAG知识文档';
