/**
 * File: server.js
 * Project: Gymlab - Ứng dụng hỗ trợ luyện tập tại nhà (Backend API)
 * Module: Xử lý Lịch tập luyện & Hệ thống Thành tựu (Phần cá nhân)
 * Author: Nguyễn Hải Nam - B22DCCN559
 * Description: Backend Server viết bằng Express.js kết nối MySQL.
 * Cung cấp các RESTful API phục vụ cho ứng dụng Mobile.
 */

const express = require('express');
const mysql = require('mysql2');
const bodyParser = require('body-parser');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(bodyParser.json());

// --- CẤU HÌNH KẾT NỐI MYSQL WORKBENCH CỦA BẠN ---
const db = mysql.createConnection({
    host: '127.0.0.1',
    user: 'root',
    password: '123456',
    database: 'gymlab'
});

db.connect(err => {
    if (err) {
        console.error('Lỗi kết nối MySQL: ' + err.message);
        return;
    }
    console.log('Đã kết nối MySQL Workbench thành công!');
});

/**
 * Hàm Helper: Lấy ID lịch tập của một ngày cụ thể. Nếu chưa có thì tự động tạo mới.
 * Sử dụng cơ chế INSERT ... ON DUPLICATE KEY UPDATE để tránh lỗi trùng lặp dữ liệu.
 * * @param {Number} userId - ID của người dùng.
 * @param {String} targetDate - Ngày cần kiểm tra/tạo lịch (YYYY-MM-DD).
 * @param {Function} callback - Callback trả về (err, scheduleId).
 */
function getOrCreateSchedule(userId, targetDate, callback) {
    const query = `
        INSERT INTO workout_schedules (user_id, date)
        VALUES (?, ?)
        ON DUPLICATE KEY UPDATE schedule_id = LAST_INSERT_ID(schedule_id)
    `;

    db.execute(query, [userId, targetDate], (err) => {
        if (err) return callback(err);

        db.query('SELECT LAST_INSERT_ID() AS schedule_id', (err2, rows) => {
            if (err2) return callback(err2);
            callback(null, rows[0].schedule_id);
        });
    });
}

// =======================================================
// CÁC API KHÔNG THUỘC PHẦN CÁ NHÂN (Giữ nguyên không comment)
// =======================================================

app.post('/login', (req, res) => {
    const { email, password } = req.body;
    const query = 'SELECT * FROM users WHERE email = ? AND password = ?';
    db.execute(query, [email, password], (err, results) => {
        if (err) return res.status(500).json({ success: false, message: err.message });
        if (results.length > 0) {
            res.json({ success: true, message: "Thành công", user: results[0] });
        } else {
            res.json({ success: false, message: "Sai tài khoản hoặc mật khẩu" });
        }
    });
});

app.post('/register', (req, res) => {
    const { username, email, password, full_name } = req.body;
    const query = 'INSERT INTO users (username, email, password, full_name) VALUES (?, ?, ?, ?)';

    db.execute(query, [username, email, password, full_name], (err, results) => {
        if (err) return res.status(500).json({ success: false, message: err.message });
        res.json({
            success: true,
            message: "Đăng ký thành công",
            user: {
                user_id: results.insertId,
                username: username,
                full_name: full_name,
                email: email
            }
        });
    });
});

app.get('/diet/:day', (req, res) => {
    const day = req.params.day;
    const query = 'SELECT * FROM diet_suggestions WHERE meal_type = ?';
    db.execute(query, [day], (err, results) => {
        if (err) return res.status(500).json({ success: false, message: err.message });
        res.json({ success: true, data: results });
    });
});

app.post('/diet/add', (req, res) => {
    const { title, calories, meal_type, user_id } = req.body;
    const query = 'INSERT INTO diet_suggestions (title, calories, meal_type, user_id) VALUES (?, ?, ?, ?)';
    db.execute(query, [title, calories, meal_type, user_id || 1], (err, results) => {
        if (err) {
            console.error("Lỗi INSERT:", err.message);
            return res.status(500).json({ success: false, message: err.message });
        }
        res.json({ success: true, message: "Đã lưu vào MySQL!" });
    });
});

app.delete('/diet/:id', (req, res) => {
    const id = req.params.id;
    const query = 'DELETE FROM diet_suggestions WHERE suggestion_id = ?';

    db.execute(query, [id], (err, result) => {
        if (err) return res.status(500).json({ success: false, message: err.message });
        if (result.affectedRows === 0) {
            return res.status(404).json({ success: false, message: 'Không tìm thấy món ăn' });
        }
        res.json({ success: true, message: 'Đã xóa món ăn!' });
    });
});

app.get('/weight/history/:userId', (req, res) => {
    const userId = req.params.userId;
    const query = 'SELECT * FROM weight_history WHERE user_id = ? ORDER BY recorded_date DESC, id DESC';
    db.execute(query, [userId], (err, results) => {
        if (err) return res.status(500).json({ success: false, message: err.message });
        res.json({ success: true, data: results });
    });
});

app.post('/weight/add', (req, res) => {
    const { weight, user_id } = req.body;
    const recorded_date = new Date().toISOString().slice(0, 10);
    const query = 'INSERT INTO weight_history (user_id, weight, recorded_date) VALUES (?, ?, ?)';
    db.execute(query, [user_id || 1, weight, recorded_date], (err, results) => {
        if (err) return res.status(500).json({ success: false, message: err.message });
        res.json({ success: true, message: "Đã lưu cân nặng thành công!" });
    });
});

// =======================================================
// CÁC API THUỘC PHẦN CÁ NHÂN (Gymlab - Workout & Achievements)
// =======================================================

/**
 * @route GET /categories
 * @description Lấy danh sách tất cả các nhóm cơ (danh mục bài tập).
 */
app.get('/categories', (req, res) => {
    const query = `
        SELECT
            id,
            name,
            description,
            image_url
        FROM categories
        ORDER BY id ASC
    `;

    db.execute(query, [], (err, results) => {
        if (err) return res.status(500).json({ message: err.message });
        res.json(results);
    });
});

/**
 * @route GET /exercises
 * @description Lấy danh sách bài tập. Có hỗ trợ lọc theo danh mục và giới hạn số lượng.
 * @query {Number} limit - (Tùy chọn) Số lượng bài tập cần lấy.
 * @query {Number} category_id - (Tùy chọn) ID nhóm cơ để lọc.
 */
app.get('/exercises', (req, res) => {
    const { limit, category_id } = req.query;

    let query = `
        SELECT
            e.exercise_id AS id,
            e.name,
            e.description,
            e.image_url,
            e.video_url,
            e.calories,
            e.difficulty_level,
            e.duration_seconds,
            e.category_id,
            c.name AS category_name
        FROM exercises e
        LEFT JOIN categories c ON e.category_id = c.id
    `;

    const params = [];
    const conditions = [];

    if (category_id) {
        conditions.push('e.category_id = ?');
        params.push(Number(category_id));
    }

    if (conditions.length > 0) {
        query += ' WHERE ' + conditions.join(' AND ');
    }

    query += ' ORDER BY e.exercise_id ASC';

    if (limit) {
        query += ' LIMIT ?';
        params.push(Number(limit));
    }

    db.execute(query, params, (err, results) => {
        if (err) {
            console.error('Lỗi /exercises:', err.message);
            return res.status(500).json({ message: err.message });
        }
        res.json(results);
    });
});

/**
 * @route GET /templates
 * @description Lấy danh sách mẫu lịch tập (bao gồm các mẫu toàn hệ thống is_global=1 và mẫu cá nhân).
 */
app.get('/templates', (req, res) => {
    const limit = req.query.limit ? Number(req.query.limit) : null;

    let query = `
        SELECT
            id,
            name,
            description,
            is_global,
            user_id,
            created_at
        FROM workout_templates
        WHERE is_global = 1 OR user_id = 1
        ORDER BY id DESC
    `;

    if (limit && !Number.isNaN(limit)) {
        query += ` LIMIT ${limit}`;
    }

    db.query(query, (err, results) => {
        if (err) {
            console.error('Lỗi /templates:', err.message);
            return res.status(500).json({ message: err.message });
        }
        res.json(results);
    });
});

/**
 * @route POST /apply-template
 * @description Áp dụng toàn bộ bài tập từ một Template vào một ngày cụ thể của User.
 * * @body {Number} user_id - ID người dùng.
 * @body {String} target_date - Ngày áp dụng (YYYY-MM-DD).
 * @body {Number} template_id - ID của mẫu lịch tập cần áp dụng.
 */
app.post('/apply-template', (req, res) => {
    const { user_id, target_date, date, template_id } = req.body;

    const userId = user_id;
    const targetDate = target_date || date;

    if (!userId || !targetDate || !template_id) {
        return res.status(400).json({
            success: false,
            message: 'Thiếu dữ liệu'
        });
    }

    // 1. Lấy hoặc tạo lịch tập cho ngày mục tiêu
    getOrCreateSchedule(userId, targetDate, (err, scheduleId) => {
        if (err) return res.status(500).json({ success: false, message: err.message });

        // 2. Đếm số lượng bài tập hiện có trong ngày để xếp index tiếp nối
        const checkExisting = `
            SELECT COUNT(*) AS total
            FROM session_exercise_details
            WHERE schedule_id = ?
        `;

        db.execute(checkExisting, [scheduleId], (err2, rows) => {
            if (err2) return res.status(500).json({ success: false, message: err2.message });

            const currentCount = rows[0].total || 0;

            // 3. Copy hàng loạt (Bulk Insert) các bài tập từ template sang lịch tập thực tế
            const insertFromTemplate = `
                INSERT INTO session_exercise_details
                (schedule_id, exercise_id, sets, reps, duration_actual, calories_burned, is_completed, order_index)
                SELECT
                    ?,
                    exercise_id,
                    sets,
                    reps,
                    0,
                    0,
                    0,
                    order_index + ?
                FROM template_exercises
                WHERE template_id = ?
                ORDER BY order_index ASC
            `;

            db.execute(insertFromTemplate, [scheduleId, currentCount, template_id], (err3) => {
                if (err3) return res.status(500).json({ success: false, message: err3.message });

                res.json({
                    success: true,
                    message: 'Đã áp dụng mẫu lịch tập!',
                    schedule_id: scheduleId
                });
            });
        });
    });
});

/**
 * @route POST /add-exercise
 * @description Thêm một bài tập lẻ vào lịch tập của ngày cụ thể.
 */
app.post('/add-exercise', (req, res) => {
    const { user_id, target_date, date, exercise_id } = req.body;

    const userId = user_id;
    const targetDate = target_date || date;

    if (!userId || !targetDate || !exercise_id) {
        return res.status(400).json({
            success: false,
            message: 'Thiếu dữ liệu'
        });
    }

    getOrCreateSchedule(userId, targetDate, (err, scheduleId) => {
        if (err) return res.status(500).json({ success: false, message: err.message });

        const countQuery = `
            SELECT COUNT(*) AS total
            FROM session_exercise_details
            WHERE schedule_id = ?
        `;

        db.execute(countQuery, [scheduleId], (err2, rows) => {
            if (err2) return res.status(500).json({ success: false, message: err2.message });

            const orderIndex = (rows[0].total || 0) + 1;

            const insertQuery = `
                INSERT INTO session_exercise_details
                (schedule_id, exercise_id, sets, reps, duration_actual, calories_burned, is_completed, order_index)
                VALUES (?, ?, 3, 12, 0, 0, 0, ?)
            `;

            db.execute(insertQuery, [scheduleId, exercise_id, orderIndex], (err3) => {
                if (err3) return res.status(500).json({ success: false, message: err3.message });

                res.json({
                    success: true,
                    message: 'Đã thêm bài tập!',
                    schedule_id: scheduleId
                });
            });
        });
    });
});

/**
 * @route GET /daily-schedule
 * @description Lấy danh sách toàn bộ các bài tập trong một ngày cụ thể của User.
 * Kết hợp JOIN giữa bảng workout_schedules, session_exercise_details và exercises.
 */
app.get('/daily-schedule', (req, res) => {
    const { user_id, date } = req.query;

    if (!user_id || !date) {
        return res.status(400).json({
            success: false,
            message: 'Thiếu user_id hoặc date'
        });
    }

    const findSchedule = `
        SELECT schedule_id
        FROM workout_schedules
        WHERE user_id = ? AND date = ?
        LIMIT 1
    `;

    db.execute(findSchedule, [user_id, date], (err, schedules) => {
        if (err) {
            console.error('Lỗi /daily-schedule:', err.message);
            return res.status(500).json({ success: false, message: err.message });
        }

        // Nếu chưa từng có lịch vào ngày này, trả về cấu trúc rỗng
        if (schedules.length === 0) {
            return res.json({
                totalCalories: 0,
                completedCount: 0,
                totalCount: 0,
                exercises: []
            });
        }

        const scheduleId = schedules[0].schedule_id;

        // Xử lý động logic hiển thị duration và calories tùy thuộc vào việc bài tập đã được tập hay chưa.
        const detailsQuery = `
            SELECT
                sed.detail_id AS detailId,
                e.name AS name,
                e.image_url AS imageUrl,
                sed.is_completed AS isCompleted,

                CASE
                    WHEN sed.is_completed = 1 AND sed.duration_actual > 0
                        THEN CONCAT(sed.duration_actual, 's')
                    WHEN e.duration_seconds IS NOT NULL AND e.duration_seconds > 0
                        THEN CONCAT(e.duration_seconds, 's')
                    ELSE '0s'
                END AS duration,

                CASE
                    WHEN sed.is_completed = 1
                        THEN ROUND(COALESCE(sed.calories_burned, 0))
                    ELSE COALESCE(e.calories, 0)
                END AS calories,

                COALESCE(sed.calories_burned, 0) AS caloriesBurned
            FROM session_exercise_details sed
            JOIN exercises e ON sed.exercise_id = e.exercise_id
            WHERE sed.schedule_id = ?
            ORDER BY sed.order_index ASC, sed.detail_id ASC
        `;

        db.execute(detailsQuery, [scheduleId], (err2, rows) => {
            if (err2) {
                console.error('Lỗi query chi tiết /daily-schedule:', err2.message);
                return res.status(500).json({ success: false, message: err2.message });
            }

            const normalizedRows = rows.map(item => ({
                detailId: item.detailId,
                name: item.name,
                imageUrl: item.imageUrl,
                duration: item.duration,
                isCompleted: Number(item.isCompleted) === 1,
                calories: Number(item.calories || 0)
            }));

            // Tính tổng Calo đã đốt
            const totalCalories = rows.reduce(
                (sum, item) => sum + Number(item.caloriesBurned || 0),
                0
            );

            const completedCount = normalizedRows.filter(item => item.isCompleted).length;

            res.json({
                totalCalories,
                completedCount,
                totalCount: normalizedRows.length,
                exercises: normalizedRows
            });
        });
    });
});

/**
 * @route POST /complete-exercise
 * @description API cốt lõi xử lý Logic sau khi tập xong 1 bài.
 * Tính toán Calories động dựa trên cân nặng, thời gian, mức độ nỗ lực.
 * Cập nhật Streak (chuỗi ngày tập) và Exp (Kinh nghiệm), Level.
 */
app.post('/complete-exercise', (req, res) => {
    console.log(req.body)
    const { detail_id, duration, mode, effort_feedback } = req.body;

    if (!detail_id || duration === undefined || duration === null) {
        return res.status(400).json({
            success: false,
            message: 'Thiếu dữ liệu'
        });
    }

    const findDetail = `
        SELECT
            sed.detail_id,
            sed.schedule_id,
            sed.exercise_id,
            sed.is_completed,
            e.calories,
            e.duration_seconds,
            ws.user_id,
            ws.date AS schedule_date,
            u.weight
        FROM session_exercise_details sed
        JOIN exercises e ON sed.exercise_id = e.exercise_id
        JOIN workout_schedules ws ON sed.schedule_id = ws.schedule_id
        JOIN users u ON ws.user_id = u.user_id
        WHERE sed.detail_id = ?
        LIMIT 1
    `;

    db.execute(findDetail, [detail_id], (err, rows) => {
        if (err) {
            return res.status(500).json({
                success: false,
                message: "Lỗi tìm bài tập: " + err.message
            });
        }

        if (rows.length === 0) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy bài tập'
            });
        }

        const row = rows[0];

        if (Number(row.is_completed) === 1) {
            return res.status(409).json({
                success: false,
                message: 'Bài tập này đã hoàn thành rồi'
            });
        }

        // --- BƯỚC 1: TÍNH TOÁN LƯỢNG CALORIES ĐÃ ĐỐT ---
        const durationNum = Number(duration);
        const baseCalories = Number(row.calories || 0);
        const baseDuration = Number(row.duration_seconds || 60);
        const userWeight = Number(row.weight) || 65.0;

        // Nội suy Calo dựa vào thời gian tập thực tế
        let rawCalories = (durationNum / baseDuration) * baseCalories;
        // Nhân thêm hệ số cân nặng so với chuẩn 65kg
        let weightAdjustedCalories = rawCalories * (userWeight / 65.0);

        // Nhân hệ số nỗ lực (feedback do người dùng đánh giá)
        let effortMultiplier = 1.0;
        const feedback = (effort_feedback !== undefined && effort_feedback !== null)
            ? Number(effort_feedback)
            : 0;

        if (feedback === -1) effortMultiplier = 0.9;
        else if (feedback === 1) effortMultiplier = 1.15;

        const finalCalories = Math.max(1, Math.round(weightAdjustedCalories * effortMultiplier));

        // Quy đổi Calo sang điểm Exp và Point
        const expEarned = finalCalories;
        const pointsEarned = finalCalories;

        // --- BƯỚC 2: CẬP NHẬT TRẠNG THÁI BÀI TẬP ---
        const updateDetail = `
            UPDATE session_exercise_details
            SET
                duration_actual = ?,
                calories_burned = ?,
                is_completed = 1,
                workout_mode = ?,
                effort_feedback = ?
            WHERE detail_id = ?
        `;

        db.execute(
            updateDetail,
            [durationNum, finalCalories, mode || 'normal', feedback, detail_id],
            (err2) => {
                if (err2) {
                    return res.status(500).json({
                        success: false,
                        message: "Lỗi cập nhật bài tập: " + err2.message
                    });
                }

                // --- BƯỚC 3: KIỂM TRA ĐIỀU KIỆN ĐỂ DUY TRÌ STREAK (>= 50% số bài trong ngày) ---
                const dailyProgressQuery = `
                    SELECT
                        COUNT(*) AS total_count,
                        SUM(CASE WHEN is_completed = 1 THEN 1 ELSE 0 END) AS completed_count
                    FROM session_exercise_details
                    WHERE schedule_id = ?
                `;

                db.execute(dailyProgressQuery, [row.schedule_id], (err3, progressRows) => {
                    if (err3) {
                        return res.status(500).json({
                            success: false,
                            message: "Lỗi tính tiến độ ngày: " + err3.message
                        });
                    }

                    const totalCount = Number(progressRows[0].total_count || 0);
                    const completedCount = Number(progressRows[0].completed_count || 0);
                    const reached50Percent =
                        totalCount > 0 && (completedCount / totalCount) >= 0.5;

                    // --- BƯỚC 4: TÍNH TOÁN VÀ CẬP NHẬT CHỈ SỐ THÀNH TỰU (UPSERT) ---
                    const getAchievementQuery = `
                        SELECT *
                        FROM achievements
                        WHERE user_id = ?
                        LIMIT 1
                    `;

                    db.execute(getAchievementQuery, [row.user_id], (err4, achRows) => {
                        if (err4) {
                            return res.status(500).json({
                                success: false,
                                message: "Lỗi lấy thành tựu: " + err4.message
                            });
                        }

                        let currentStreak = 0;
                        let longestStreak = 0;
                        let totalPoints = 0;
                        let totalExp = 0;
                        let totalTime = 0;
                        let lastWorkoutDate = null;

                        if (achRows.length > 0) {
                            const ach = achRows[0];
                            currentStreak = ach.current_streak || 0;
                            longestStreak = ach.longest_streak || 0;
                            totalPoints = ach.total_points || 0;
                            totalExp = ach.total_exp || 0;
                            totalTime = ach.total_time || 0;
                            lastWorkoutDate = ach.last_workout_date;
                        }

                        let newCurrentStreak = currentStreak;

                        // Chỉ cộng/giữ Streak nếu hôm nay tập đạt đủ >= 50% khối lượng
                        if (reached50Percent) {
                            const today = new Date(row.schedule_date);
                            today.setHours(0, 0, 0, 0);

                            if (lastWorkoutDate) {
                                const lastDate = new Date(lastWorkoutDate);
                                lastDate.setHours(0, 0, 0, 0);

                                const diffDays = Math.round(
                                    (today - lastDate) / (1000 * 60 * 60 * 24)
                                );

                                if (diffDays === 0) {
                                    // hôm nay đã từng được tính streak rồi -> giữ nguyên
                                    newCurrentStreak = currentStreak || 1;
                                } else if (diffDays === 1) {
                                    newCurrentStreak = currentStreak + 1; // Tập liên tiếp
                                } else {
                                    newCurrentStreak = 1; // Mất chuỗi, làm lại từ đầu
                                }
                            } else {
                                newCurrentStreak = 1;
                            }
                        }

                        const newLongestStreak = Math.max(longestStreak, newCurrentStreak);
                        const newTotalPoints = totalPoints + pointsEarned;
                        const newTotalExp = totalExp + expEarned;
                        const newTotalTime = totalTime + durationNum;

                        // Tính level (1000 exp = 1 level)
                        const newLevel = Math.floor(newTotalExp / 1000) + 1;

                        const upsertAchievement = `
                            INSERT INTO achievements
                            (
                                user_id,
                                current_streak,
                                longest_streak,
                                total_points,
                                total_exp,
                                level,
                                total_time,
                                last_activity_date,
                                last_workout_date
                            )
                            VALUES (?, ?, ?, ?, ?, ?, ?, CURDATE(), ?)
                            ON DUPLICATE KEY UPDATE
                                current_streak = VALUES(current_streak),
                                longest_streak = VALUES(longest_streak),
                                total_points = VALUES(total_points),
                                total_exp = VALUES(total_exp),
                                level = VALUES(level),
                                total_time = VALUES(total_time),
                                last_activity_date = CURDATE(),
                                last_workout_date = VALUES(last_workout_date)
                        `;

                        // Chỉ update last_workout_date nếu đạt >= 50%
                        const workoutDateForStreak = reached50Percent ? row.schedule_date : lastWorkoutDate;

                        db.execute(
                            upsertAchievement,
                            [
                                row.user_id,
                                newCurrentStreak,
                                newLongestStreak,
                                newTotalPoints,
                                newTotalExp,
                                newLevel,
                                newTotalTime,
                                workoutDateForStreak
                            ],
                            (err5) => {
                                if (err5) {
                                    return res.status(500).json({
                                        success: false,
                                        message: "Lỗi lưu thành tựu: " + err5.message
                                    });
                                }

                                res.json({
                                    success: true,
                                    message: 'Hoàn thành bài tập!',
                                    calories: finalCalories,
                                    exp: expEarned,
                                    points: pointsEarned,
                                    streak: newCurrentStreak,
                                    reached50Percent
                                });
                            }
                        );
                    });
                });
            }
        );
    });
});

/**
 * @route POST /workout-templates
 * @description Tạo mới một mẫu lịch tập cá nhân.
 */
app.post('/workout-templates', (req, res) => {
    const { user_id, name, description, exercises } = req.body;
    const userId = user_id || 1;

    if (!name || !Array.isArray(exercises) || exercises.length === 0) {
        return res.status(400).json({
            success: false,
            message: 'Thiếu dữ liệu tạo mẫu'
        });
    }

    const insertTemplate = `
        INSERT INTO workout_templates (user_id, name, description, is_global)
        VALUES (?, ?, ?, 0)
    `;

    db.execute(insertTemplate, [userId, name, description || null], (err, result) => {
        if (err) return res.status(500).json({ success: false, message: err.message });

        const templateId = result.insertId;

        // Chuẩn bị dữ liệu Bulk Insert cho bảng template_exercises
        const values = exercises.map((ex, index) => [
            templateId,
            ex.exerciseId || ex.exercise_id,
            ex.sets || 3,
            ex.reps || 12,
            ex.orderIndex || ex.order_index || (index + 1)
        ]);

        const insertDetails = `
            INSERT INTO template_exercises (template_id, exercise_id, sets, reps, order_index)
            VALUES ?
        `;

        db.query(insertDetails, [values], (err2) => {
            if (err2) return res.status(500).json({ success: false, message: err2.message });

            res.json({
                success: true,
                message: 'Đã tạo mẫu lịch tập!',
                template_id: templateId
            });
        });
    });
});

/**
 * @route GET /achievements/:userId
 * @description Trả về thống kê tổng quát của người dùng và danh sách trạng thái huy hiệu.
 * Trạng thái huy hiệu (is_earned) được tính toán động (dynamic mapping) thay vì lưu tĩnh trong DB.
 */
app.get('/achievements/:userId', (req, res) => {
    const userId = req.params.userId;

    // 1. Lấy chỉ số hiện tại của user từ bảng achievements
    const statsQuery = 'SELECT * FROM achievements WHERE user_id = ? LIMIT 1';

    db.execute(statsQuery, [userId], (err, statsRows) => {
        if (err) return res.status(500).json({ success: false, message: err.message });

        const stats = statsRows.length > 0 ? statsRows[0] : {
            level: 1, total_exp: 0, current_streak: 0, longest_streak: 0, total_points: 0
        };

        // 2. Lấy danh sách toàn bộ huy hiệu để so sánh điều kiện
        const badgesQuery = 'SELECT * FROM badges';

        db.execute(badgesQuery, [], (err2, badgesRows) => {
            if (err2) return res.status(500).json({ success: false, message: err2.message });

            // Logic: Duyệt qua từng huy hiệu, kiểm tra xem user đạt điều kiện chưa
            const badges = badgesRows.map(badge => {
                const isEarned = (stats.total_points >= badge.required_points) &&
                                 (stats.longest_streak >= badge.required_streak);
                return {
                    ...badge,
                    is_earned: isEarned
                };
            });

            res.json({
                success: true,
                stats: stats,
                badges: badges
            });
        });
    });
});

console.log("ROUTE READY: GET /categories");
console.log("ROUTE READY: GET /exercises");
console.log("ROUTE READY: GET /templates");
console.log("ROUTE READY: GET /daily-schedule");
console.log("ROUTE READY: POST /workout-templates");

app.listen(3000, () => {
    console.log('Server Gymlab đang chạy tại http://localhost:3000');
});