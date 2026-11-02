/* ======================== Config ======================== */
const API_BASE = '/api';
function getToken() { return localStorage.getItem('adminToken') || ''; }
async function api(path, opts) {
  opts = opts || {};
  var headers = { 'Content-Type': 'application/json' };
  var token = getToken();
  if (token) headers['Authorization'] = token;
  try {
    var res = await fetch(API_BASE + path, Object.assign({}, opts, { headers: headers }));
    var text = await res.text();
    var data;
    try { data = JSON.parse(text); } catch(e) { throw new Error('响应格式错误: ' + text.substring(0,200)); }
    if (data.code === 401) {
      localStorage.removeItem('adminToken');
      window.location.hash = '';
      renderApp();
      return null;
    }
    if (data.code !== 200) throw new Error(data.message || '请求失败');
    return data.data;
  } catch(e) { throw e; }
}
function toast(msg, type) {
  type = type || 'info';
  var c = document.querySelector('.toast-container');
  if (!c) { c = document.createElement('div'); c.className = 'toast-container'; document.body.appendChild(c); }
  var t = document.createElement('div'); t.className = 'toast ' + type; t.textContent = msg;
  c.appendChild(t);
  setTimeout(function() { t.style.opacity = '0'; t.style.transition = 'opacity 0.3s'; setTimeout(function() { t.remove(); }, 300); }, 3000);
}
function fmtTime(t) { if (!t) return ''; try { return new Date(t).toLocaleString('zh-CN'); } catch(e) { return t; } }
function fmtDate(d) { if (!d) return ''; try { return new Date(d).toLocaleDateString('zh-CN'); } catch(e) { return d; } }
function html(str) { return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }
function statusBadge(status) {
  var map = { pending:'pending', approved:'approved', rejected:'rejected', done:'done', present:'present', absent:'absent', late:'late', normal:'normal', waiting:'waiting', ready:'ready', none:'none', active:'active', inactive:'inactive' };
  var labelMap = { pending:'待审批', approved:'已通过', rejected:'已驳回', done:'已完成', present:'已签到', absent:'旷课', late:'迟到', normal:'正常', waiting:'等待中', ready:'可签到', none:'无', active:'正常', inactive:'已禁用' };
  var cls = map[status] || '';
  var label = labelMap[status] || status;
  return '<span class="status-badge ' + cls + '">' + label + '</span>';
}
function paginationHTML(page, totalPages, total, pfn) {
  if (!totalPages || totalPages <= 1) return '';
  var s = 'onclick="paginationGo(this)" data-p=' + page + ' data-fn=' + pfn;
  var h = '<div class="pagination">';
  h += '<button ' + s + ' data-page=' + (page-1) + (page<=1?' disabled':'') + '>上一页</button>';
  for (var i = Math.max(1,page-2); i <= Math.min(totalPages,page+2); i++) {
    h += '<button class="' + (i===page?'active':'') + '" ' + s + ' data-page=' + i + '>' + i + '</button>';
  }
  h += '<button ' + s + ' data-page=' + (page+1) + (page>=totalPages?' disabled':'') + '>下一页</button>';
  h += '<span class="page-info">共 ' + total + ' 条</span></div>';
  return h;
}
function paginationGo(el) {
  var page = parseInt(el.getAttribute('data-page'));
  var fnName = el.getAttribute('data-fn');
  window[fnName](page);
}
function showModal(title, bodyHTML, onConfirm) {
  var overlay = document.createElement('div'); overlay.className = 'modal-overlay';
  var btns = '<div class="modal-actions"><button class="btn-outline" onclick="this.closest(\'.modal-overlay\').remove()">取消</button>';
  if (onConfirm) btns += '<button class="btn-primary" id="modalConfirmBtn">确认</button>';
  btns += '</div>';
  overlay.innerHTML = '<div class="modal-box"><h3>' + html(title) + '</h3>' + bodyHTML + btns + '</div>';
  document.body.appendChild(overlay);
  overlay.addEventListener('click', function(e) { if (e.target === overlay) overlay.remove(); });
  if (onConfirm) {
    document.getElementById('modalConfirmBtn').addEventListener('click', function() {
      Promise.resolve().then(function() { return onConfirm(); }).then(function() { overlay.remove(); })
      .catch(function(e) { toast(e.message, 'error'); });
    });
  }
  return overlay;
}
function loadingHTML() { return '<div class="loading-state"><div class="spinner"></div><p>加载中...</p></div>'; }


var _pages={dash:1,leaves:1,repair:1,users:1,ann:1,fb:1};

function renderAttendance() {
  var el = document.getElementById('pageContent');
  el.innerHTML = loadingHTML();
  var p1 = api('/teacher/checkin/today-summary');
  var p2 = api('/course/schedule');
  Promise.all([p1, p2]).then(function(vals) {
    var ts = vals[0];
    var cs = vals[1];
    var ckd = (ts && ts.classChecked) || 0;
    var tot = (ts && ts.totalStudents) || 0;
    var rate = tot > 0 ? Math.round(ckd / tot * 100) : 0;
    var rows = '';
    if (cs && cs.length) {
      for (var i = 0; i < cs.length; i++) {
        var c = cs[i];
        rows += '<tr>';
        rows += '<td>' + html(c.name) + '</td>';
        rows += '<td>' + html(c.teacher) + '</td>';
        rows += '<td>' + html(c.weekDay) + '</td>';
        rows += '<td>' + html(c.startTime) + '-' + html(c.endTime) + '</td>';
        rows += '<td>' + html(c.location) + '</td>';
        rows += '</tr>';
      }
    }
    el.innerHTML = '<div class="stats-grid">';
    el.innerHTML += '<div class="stat-card stat-green"><div class="stat-label">今日已签到</div><div class="stat-value">' + ckd + '/' + tot + '</div></div>';
    el.innerHTML += '<div class="stat-card stat-red"><div class="stat-label">未签到</div><div class="stat-value">' + (tot - ckd) + '</div></div>';
    el.innerHTML += '<div class="stat-card stat-accent"><div class="stat-label">签到率</div><div class="stat-value">' + rate + '%</div></div>';
    el.innerHTML += '<div class="stat-card stat-blue"><div class="stat-label">课程数</div><div class="stat-value">' + (cs ? cs.length : 0) + '</div></div>';
    el.innerHTML += '</div>';
    el.innerHTML += '<div class="section-card"><div class="card-header"><h3>课程表</h3></div><div class="card-body"><div class="table-wrapper"><table><tr><th>课程</th><th>教师</th><th>星期</th><th>时间</th><th>地点</th></tr>';
    if (rows) { el.innerHTML += rows; } else { el.innerHTML += '<tr><td colspan="5" class="text-center">暂无课程</td></tr>'; }
    el.innerHTML += '</table></div></div></div>';
  }).catch(function(e) {
    el.innerHTML = '<div class="empty-state"><p>错误：' + html(e.message) + '</p></div>';
  });
}

function renderIntern() {
  var el = document.getElementById('pageContent');
  el.innerHTML = loadingHTML();
  api('/intern/admin/history?page=1&size=50').then(function(data) {
    var rows = '';
    var records = (data && data.records) || [];
    for (var i = 0; i < records.length; i++) {
      var r = records[i];
      rows += '<tr>';
      rows += '<td>' + (r.id || '') + '</td>';
      rows += '<td>' + html(r.userName || r.userId || '') + '</td>';
      rows += '<td>' + html(r.company || '') + '</td>';
      rows += '<td>' + fmtDate(r.date) + '</td>';
      rows += '<td>' + fmtTime(r.checkinTime) + '</td>';
      rows += '<td>' + html(r.locationAddr || '') + '</td>';
      rows += '<td>' + statusBadge(r.status) + '</td>';
      rows += '</tr>';
    }
    el.innerHTML = '<div class="section-card"><div class="card-header"><h3>实习打卡</h3></div><div class="card-body"><div class="table-wrapper"><table><tr><th>ID</th><th>学生</th><th>公司</th><th>日期</th><th>时间</th><th>地点</th><th>状态</th></tr>';
    if (rows) { el.innerHTML += rows; } else { el.innerHTML += '<tr><td colspan="7" class="text-center">暂无记录</td></tr>'; }
    el.innerHTML += '</table></div></div></div>';
  }).catch(function(e) {
    el.innerHTML = '<div class="empty-state"><p>错误：' + html(e.message) + '</p></div>';
  });
}
function renderLeaves() {
  _pages.leaves = _pages.leaves || 1;
  var el = document.getElementById('pageContent');
  el.innerHTML = loadingHTML();
  var p1 = api('/leaves/teacher/all?page=' + _pages.leaves + '&size=20&status=all');
  var p2 = api('/leaves/teacher/stats');
  Promise.all([p1, p2]).then(function(vals) {
    var data = vals[0];
    var stats = vals[1];
    var rows = '';
    var records = (data && data.records) || [];
    var typeMap = { sick: '病假', personal: '事假', official: '公假', annual: '年假' };
    for (var i = 0; i < records.length; i++) {
      var l = records[i];
      rows += '<tr>';
      rows += '<td>' + (l.id || '') + '</td>';
      rows += '<td>' + html(l.studentName || '') + '</td>';
      rows += '<td>' + html(l.studentId || '') + '</td>';
      rows += '<td>' + (typeMap[l.type] || l.type || '') + '</td>';
      rows += '<td>' + fmtDate(l.startDate) + '~' + fmtDate(l.endDate) + '</td>';
      rows += '<td>' + (l.days || '') + '</td>';
      rows += '<td>' + html(String(l.reason || '').substring(0, 20)) + '</td>';
      rows += '<td>' + statusBadge(l.status) + '</td>';
      rows += '<td>' + fmtTime(l.createdAt) + '</td>';
      rows += '</tr>';
    }
    var statsHtml = '<div class="stats-grid">';
    statsHtml += '<div class="stat-card stat-orange"><div class="stat-label">待审批</div><div class="stat-value">' + ((stats && stats.pending) || 0) + '</div></div>';
    statsHtml += '<div class="stat-card stat-green"><div class="stat-label">已通过</div><div class="stat-value">' + ((stats && stats.approved) || 0) + '</div></div>';
    statsHtml += '<div class="stat-card stat-red"><div class="stat-label">已驳回</div><div class="stat-value">' + ((stats && stats.rejected) || 0) + '</div></div>';
    statsHtml += '<div class="stat-card stat-blue"><div class="stat-label">总计</div><div class="stat-value">' + ((stats && stats.total) || 0) + '</div></div>';
    statsHtml += '</div>';
    var pag = paginationHTML(_pages.leaves, data.pages || 1, data.total || 0, 'renderLeavesGo');
    el.innerHTML = statsHtml;
    el.innerHTML += '<div class="section-card"><div class="card-header"><h3>请假记录</h3></div><div class="card-body"><div class="table-wrapper"><table><tr><th>ID</th><th>学生</th><th>学号</th><th>类型</th><th>日期</th><th>天数</th><th>事由</th><th>状态</th><th>时间</th></tr>';
    if (rows) { el.innerHTML += rows; } else { el.innerHTML += '<tr><td colspan="9" class="text-center">暂无记录</td></tr>'; }
    el.innerHTML += '</table></div>' + pag + '</div></div>';
  }).catch(function(e) {
    el.innerHTML = '<div class="empty-state"><p>错误：' + html(e.message) + '</p></div>';
  });
}
function renderLeavesGo(p) { _pages.leaves = p; renderLeaves(); }
function renderRepairs() {
  _pages.repair = _pages.repair || 1;
  var el = document.getElementById('pageContent');
  el.innerHTML = loadingHTML();
  var p1 = api('/repair/list?page=' + _pages.repair + '&size=20&status=all');
  var p2 = api('/repair/stats');
  Promise.all([p1, p2]).then(function(vals) {
    var data = vals[0];
    var stats = vals[1];
    var rows = '';
    var records = (data && data.records) || [];
    for (var i = 0; i < records.length; i++) {
      var r = records[i];
      rows += '<tr>';
      rows += '<td>' + (r.id || '') + '</td>';
      rows += '<td>' + html(r.studentName || '') + '</td>';
      rows += '<td>' + html(r.dormBuilding || '') + ' ' + html(r.dormRoom || '') + '</td>';
      rows += '<td>' + html(r.category || '') + '</td>';
      rows += '<td>' + html(String(r.description || '').substring(0, 30)) + '</td>';
      rows += '<td>' + html(r.phone || '') + '</td>';
      rows += '<td>' + statusBadge(r.status) + '</td>';
      rows += '<td>' + fmtTime(r.createdAt) + '</td>';
      rows += '<td><button class="action-btn" onclick="handleRepair(' + r.id + ')" title="处理">&#10003;</button></td>';
      rows += '</tr>';
    }
    var statsHtml = '<div class="stats-grid">';
    statsHtml += '<div class="stat-card stat-orange"><div class="stat-label">待处理</div><div class="stat-value">' + ((stats && stats.pending) || 0) + '</div></div>';
    statsHtml += '<div class="stat-card stat-green"><div class="stat-label">已完成</div><div class="stat-value">' + ((stats && stats.done) || 0) + '</div></div>';
    statsHtml += '<div class="stat-card stat-blue"><div class="stat-label">总计</div><div class="stat-value">' + ((stats && stats.total) || 0) + '</div></div>';
    var rate = (stats && stats.total > 0) ? Math.round(stats.done / stats.total * 100) : 0;
    statsHtml += '<div class="stat-card"><div class="stat-label">完成率</div><div class="stat-value">' + rate + '%</div></div>';
    statsHtml += '</div>';
    var pag = paginationHTML(_pages.repair, data.pages || 1, data.total || 0, 'renderRepairsGo');
    el.innerHTML = statsHtml;
    el.innerHTML += '<div class="section-card"><div class="card-header"><h3>报修记录</h3></div><div class="card-body"><div class="table-wrapper"><table><tr><th>ID</th><th>学生</th><th>宿舍</th><th>类别</th><th>描述</th><th>电话</th><th>状态</th><th>时间</th><th>操作</th></tr>';
    if (rows) { el.innerHTML += rows; } else { el.innerHTML += '<tr><td colspan="9" class="text-center">暂无记录</td></tr>'; }
    el.innerHTML += '</table></div>' + pag + '</div></div>';
  }).catch(function(e) {
    el.innerHTML = '<div class="empty-state"><p>错误：' + html(e.message) + '</p></div>';
  });
}
function renderRepairsGo(p) { _pages.repair = p; renderRepairs(); }
function handleRepair(id) {
  var body = '<div class="form-group"><label>状态</label><select id="repairStatus"><option value="done">已完成</option><option value="pending">待处理</option></select></div>';
  body += '<div class="form-group"><label>回复</label><textarea id="repairReply"></textarea></div>';
  showModal('处理报修', body, function() {
    var status = document.getElementById('repairStatus').value;
    var reply = document.getElementById('repairReply').value;
    return api('/repair/handle/' + id, { method: 'PUT', body: JSON.stringify({ status: status, reply: reply }) }).then(function() {
      toast('报修已更新', 'success');
      renderRepairs();
    });
  });
}
function renderUsers() {
  _pages.users = _pages.users || 1;
  var el = document.getElementById('pageContent');
  el.innerHTML = loadingHTML();
  api('/admin/users?page=' + _pages.users + '&size=20').then(function(data) {
    var rows = '';
    var records = (data && data.records) || [];
    for (var i = 0; i < records.length; i++) {
      var u = records[i];
      rows += '<tr>';
      rows += '<td>' + (u.id || '') + '</td>';
      rows += '<td>' + html(u.name || '') + '</td>';
      rows += '<td>' + html(u.studentId || '') + '</td>';
      rows += '<td>' + html(u.college || '') + '</td>';
      rows += '<td>' + html(u.major || '') + '</td>';
      rows += '<td>' + html(u.grade || '') + '</td>';
      rows += '<td>' + statusBadge(u.role) + '</td>';
      rows += '<td>' + (u.status === 1 ? statusBadge('active') : statusBadge('inactive')) + '</td>';
      rows += '<td>' + fmtTime(u.createdAt) + '</td>';
      rows += '<td><button class="action-btn" onclick="editUser(' + u.id + ')" title="编辑">&#9998;</button></td>';
      rows += '</tr>';
    }
    var filterHtml = '<div class="filter-bar"><button class="btn-primary" onclick="showAddUser()">+ 添加用户</button>';
    filterHtml += '<button class="btn-outline" onclick="_pages.users=1;renderUsers()">刷新</button></div>';
    var pag = paginationHTML(_pages.users, data.pages || 1, data.total || 0, 'renderUsersGo');
    el.innerHTML = filterHtml;
    el.innerHTML += '<div class="section-card"><div class="card-header"><h3>用户列表</h3><span style="font-size:12px;color:#6b7280;">共 ' + (data.total || 0) + ' 人</span></div><div class="card-body"><div class="table-wrapper"><table><tr><th>ID</th><th>姓名</th><th>学号</th><th>学院</th><th>专业</th><th>年级</th><th>角色</th><th>状态</th><th>创建时间</th><th>操作</th></tr>';
    if (rows) { el.innerHTML += rows; } else { el.innerHTML += '<tr><td colspan="10" class="text-center">暂无用户</td></tr>'; }
    el.innerHTML += '</table></div>' + pag + '</div></div>';
  }).catch(function(e) {
    el.innerHTML = '<div class="empty-state"><p>错误：' + html(e.message) + '</p></div>';
  });
}
function renderUsersGo(p) { _pages.users = p; renderUsers(); }
function showAddUser() {
  var body = '<div class="form-group"><label>姓名</label><input id="uName"></div>';
  body += '<div class="form-group"><label>学号</label><input id="uSid"></div>';
  body += '<div class="form-group"><label>学院</label><input id="uCollege"></div>';
  body += '<div class="form-group"><label>专业</label><input id="uMajor"></div>';
  body += '<div class="form-group"><label>密码</label><input id="uPass" value="123456"></div>';
  body += '<div class="form-group"><label>角色</label><select id="uRole"><option value="student">学生</option><option value="teacher">教师</option></select></div>';
  showModal('添加用户', body, function() {
    return api('/admin/user', { method: 'POST', body: JSON.stringify({
      name: document.getElementById('uName').value,
      studentId: document.getElementById('uSid').value,
      college: document.getElementById('uCollege').value,
      major: document.getElementById('uMajor').value,
      password: document.getElementById('uPass').value || '123456',
      role: document.getElementById('uRole').value
    })}).then(function() {
      toast('用户已创建', 'success');
      renderUsers();
    });
  });
}
function editUser(id) {
  api('/admin/users?page=1&size=1000').then(function(data) {
    var users = (data && data.records) || [];
    var u = null;
    for (var i = 0; i < users.length; i++) { if (users[i].id === id) { u = users[i]; break; } }
    if (!u) { toast('未找到用户', 'error'); return; }
    var body = '<div class="form-group"><label>姓名</label><input id="uName" value="' + html(u.name || '') + '"></div>';
    body += '<div class="form-group"><label>学号</label><input id="uSid" value="' + html(u.studentId || '') + '"></div>';
    body += '<div class="form-group"><label>学院</label><input id="uCollege" value="' + html(u.college || '') + '"></div>';
    body += '<div class="form-group"><label>角色</label><select id="uRole"><option value="student"' + (u.role==='student'?' selected':'') + '>学生</option><option value="teacher"' + (u.role==='teacher'?' selected':'') + '>教师</option></select></div>';
    showModal('编辑用户 #' + id, body, function() {
      return api('/admin/user/' + id, { method: 'PUT', body: JSON.stringify({
        name: document.getElementById('uName').value,
        studentId: document.getElementById('uSid').value,
        college: document.getElementById('uCollege').value,
        role: document.getElementById('uRole').value
      })}).then(function() {
        toast('用户已更新', 'success');
        renderUsers();
      });
    });
  });
}
function renderAnnouncements() {
  _pages.ann = _pages.ann || 1;
  var el = document.getElementById('pageContent');
  el.innerHTML = loadingHTML();
  api('/announcements?page=' + _pages.ann + '&size=20').then(function(data) {
    var rows = '';
    var records = (data && data.records) || [];
    for (var i = 0; i < records.length; i++) {
      var a = records[i];
      rows += '<tr>';
      rows += '<td>' + (a.id || '') + '</td>';
      rows += '<td>' + html(a.title || '') + '</td>';
      rows += '<td>' + html(a.typeName || a.type || '') + '</td>';
      rows += '<td>' + html(String(a.summary || '').substring(0, 40)) + '</td>';
      rows += '<td>' + html(a.publisher || '') + '</td>';
      rows += '<td>' + (a.isTop ? '&#9733; 置顶' : '') + '</td>';
      rows += '<td>' + (a.readCount || 0) + '</td>';
      rows += '<td>' + fmtDate(a.createdAt) + '</td>';
      rows += '</tr>';
    }
    var filterHtml = '<div class="filter-bar"><button class="btn-primary" onclick="showAddAnn()">+ 新建公告</button>';
    filterHtml += '<button class="btn-outline" onclick="_pages.ann=1;renderAnnouncements()">刷新</button></div>';
    var pag = paginationHTML(_pages.ann, data.pages || 1, data.total || 0, 'renderAnnGo');
    el.innerHTML = filterHtml;
    el.innerHTML += '<div class="section-card"><div class="card-header"><h3>公告列表</h3><span style="font-size:12px;color:#6b7280;">共 ' + (data.total || 0) + ' 条</span></div><div class="card-body"><div class="table-wrapper"><table><tr><th>ID</th><th>标题</th><th>类型</th><th>摘要</th><th>发布者</th><th>置顶</th><th>阅读量</th><th>日期</th></tr>';
    if (rows) { el.innerHTML += rows; } else { el.innerHTML += '<tr><td colspan="8" class="text-center">暂无公告</td></tr>'; }
    el.innerHTML += '</table></div>' + pag + '</div></div>';
  }).catch(function(e) {
    el.innerHTML = '<div class="empty-state"><p>错误：' + html(e.message) + '</p></div>';
  });
}
function renderAnnGo(p) { _pages.ann = p; renderAnnouncements(); }
function showAddAnn() {
  var body = '<div class="form-group"><label>标题</label><input id="annTitle"></div>';
  body += '<div class="form-group"><label>类型</label><select id="annType"><option value="notice">通知</option><option value="activity">活动</option><option value="academic">学术</option></select></div>';
  body += '<div class="form-group"><label>摘要</label><textarea id="annSummary"></textarea></div>';
  body += '<div class="form-group"><label>内容</label><textarea id="annContent"></textarea></div>';
  showModal('新建公告', body, function() {
    return api('/announcements', { method: 'POST', body: JSON.stringify({
      title: document.getElementById('annTitle').value,
      type: document.getElementById('annType').value,
      summary: document.getElementById('annSummary').value,
      content: document.getElementById('annContent').value
    })}).then(function() {
      toast('公告已创建', 'success');
      renderAnnouncements();
    });
  });
}
function renderFeedback() {
  _pages.fb = _pages.fb || 1;
  var el = document.getElementById('pageContent');
  el.innerHTML = loadingHTML();
  api('/feedback/teacher?page=' + _pages.fb + '&size=20').then(function(data) {
    var rows = '';
    var records = (data && data.records) || [];
    for (var i = 0; i < records.length; i++) {
      var f = records[i];
      rows += '<tr>';
      rows += '<td>' + (f.id || '') + '</td>';
      rows += '<td>' + html(f.type || '') + '</td>';
      rows += '<td>' + html(String(f.content || '').substring(0, 50)) + '</td>';
      rows += '<td>' + html(f.contact || '') + '</td>';
      rows += '<td>' + statusBadge(f.status) + '</td>';
      rows += '<td>' + html(String(f.reply || '').substring(0, 30)) + '</td>';
      rows += '<td>' + fmtTime(f.createdAt) + '</td>';
      rows += '<td>';
      if (f.status === 'pending') { rows += '<button class="action-btn" onclick="replyFb(' + f.id + ')">&#9993;</button>'; }
      rows += '</td>';
      rows += '</tr>';
    }
    var pag = paginationHTML(_pages.fb, data.pages || 1, data.total || 0, 'renderFbGo');
    el.innerHTML = '<div class="section-card"><div class="card-header"><h3>意见反馈</h3><span style="font-size:12px;color:#6b7280;">共 ' + (data.total || 0) + ' 条</span></div><div class="card-body"><div class="table-wrapper"><table><tr><th>ID</th><th>类型</th><th>内容</th><th>联系方式</th><th>状态</th><th>回复</th><th>时间</th><th>操作</th></tr>';
    if (rows) { el.innerHTML += rows; } else { el.innerHTML += '<tr><td colspan="8" class="text-center">暂无反馈</td></tr>'; }
    el.innerHTML += '</table></div>' + pag + '</div></div>';
  }).catch(function(e) {
    el.innerHTML = '<div class="empty-state"><p>错误：' + html(e.message) + '</p></div>';
  });
}
function renderFbGo(p) { _pages.fb = p; renderFeedback(); }
function replyFb(id) {
  var body = '<div class="form-group"><label>回复</label><textarea id="fbReply"></textarea></div>';
  showModal('回复反馈', body, function() {
    var reply = document.getElementById('fbReply').value;
    return api('/feedback/reply/' + id, { method: 'PUT', body: JSON.stringify({ reply: reply }) }).then(function() {
      toast('回复已发送', 'success');
      renderFeedback();
    });
  });
}
var ROUTES = {
  dashboard: { label: '仪表盘', page: 'overview', icon: 'home' },
  attendance: { label: '上课打卡', page: 'checkin', icon: 'clipboard' },
  dorm: { label: '查寝打卡', page: 'checkin', icon: 'home2' },
  intern: { label: '实习打卡', page: 'checkin', icon: 'briefcase' },
  leaves: { label: '请假管理', page: 'manage', icon: 'calendar' },
  repairs: { label: '报修管理', page: 'manage', icon: 'tool' },
  users: { label: '用户管理', page: 'manage', icon: 'users' },
  announcements: { label: '公告管理', page: 'content', icon: 'megaphone' },
  feedback: { label: '反馈管理', page: 'content', icon: 'message' }
};
var ICON_MAP = {
  home: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>',
  clipboard: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/></svg>',
  home2: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></svg>',
  briefcase: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="7" width="20" height="14" rx="2" ry="2"/><path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/></svg>',
  calendar: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>',
  tool: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>',
  users: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>',
  megaphone: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 9 3 14 13 21 13 2"/><line x1="22" y1="9" x2="22" y2="14"/></svg>',
  message: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>'
};
var PAGE_NAMES = { overview: '概览', checkin: '打卡管理', manage: '管理', content: '内容管理' };
function renderApp() {
  var hash = window.location.hash.replace('#', '') || 'dashboard';
  var token = getToken();
  if (!token) {
    document.getElementById('appRoot').innerHTML = '<div class="login-wrapper"><div class="login-card"><h2>校园考勤助手</h2><p class="login-sub">管理后台</p><div class="form-group"><label>学号</label><input id="loginUser" value="admin"></div><div class="form-group"><label>密码</label><input id="loginPass" type="password" value="123456" onkeydown="if(event.key===\'Enter\')doLogin()"></div><button class="login-btn" id="loginBtn" onclick="doLogin()">登录</button><div class="login-error" id="loginError"></div></div></div>';
    return;
  }
  var route = ROUTES[hash];
  var navHTML = '';
  var lastPage = '';
  for (var key in ROUTES) {
    var r = ROUTES[key];
    if (r.page !== lastPage) { navHTML += '<div class="nav-section">' + PAGE_NAMES[r.page] + '</div>'; lastPage = r.page; }
    var active = (key === hash) ? ' active' : '';
    navHTML += '<div class="nav-item' + active + '" onclick="navigateTo(\'' + key + '\')">' + (ICON_MAP[r.icon] || '') + '<span>' + r.label + '</span></div>';
  }
  document.getElementById('appRoot').innerHTML = '<div class="app-layout"><div class="sidebar"><div class="sidebar-brand"><h1>校园考勤助手</h1><p>管理后台</p></div><div class="sidebar-nav">' + navHTML + '</div></div><div class="main-area"><div class="topbar"><div class="topbar-title">' + (route ? route.label : '仪表盘') + '</div><div class="topbar-right"><span class="user-badge">管理员</span><button class="logout-btn" onclick="logout()">退出登录</button></div></div><div class="content-area" id="pageContent">' + loadingHTML() + '</div></div></div>';
  if (route) {
    var fnMap = { dashboard: renderDashboard, attendance: renderAttendance, dorm: renderDorm, intern: renderIntern, leaves: renderLeaves, repairs: renderRepairs, users: renderUsers, announcements: renderAnnouncements, feedback: renderFeedback };
    var fn = fnMap[hash];
    if (fn) { fn(); }
  }
}
function navigateTo(route) { window.location.hash = route; }
function doLogin() {
  var user = document.getElementById('loginUser').value;
  var pass = document.getElementById('loginPass').value;
  var btn = document.getElementById('loginBtn');
  var err = document.getElementById('loginError');
  btn.disabled = true; btn.textContent = '登录中...';
  api('/auth/login', { method: 'POST', body: JSON.stringify({ studentId: user, password: pass }) }).then(function(result) {
    localStorage.setItem('adminToken', result.token);
    renderApp();
  }).catch(function(e) {
    err.textContent = e.message || '登录失败';
    btn.disabled = false; btn.textContent = '登录';
  });
}
function logout() { localStorage.removeItem('adminToken'); renderApp(); }
window.addEventListener('hashchange', renderApp);
renderApp();

function renderDashboard() {
  var el = document.getElementById('pageContent');
  el.innerHTML = loadingHTML();
  var p1 = api('/admin/dashboard');
  var p2 = api('/teacher/checkin/today-summary');
  var p3 = api('/leaves/teacher/stats');
  var p4 = api('/repair/stats');
  var p5 = api('/admin/stats');
  Promise.all([p1, p2, p3, p4, p5]).then(function(vals) {
    var d = vals[0] || {};
    var ts = vals[1] || {};
    var ls = vals[2] || {};
    var rs = vals[3] || {};
    var as = vals[4] || {};
    var sc = as.studentCount || 0;
    var tc = as.teacherCount || 0;
    var ckd = ts.classChecked || 0;
    var ckn = ts.classNotChecked || 0;
    var tot = ts.totalStudents || 0;
    var dck = ts.dormChecked || 0;
    var cr = tot > 0 ? Math.round(ckd / tot * 100) : 0;
    var dr = tot > 0 ? Math.round(dck / tot * 100) : 0;
    el.innerHTML = '<div class="stats-grid">';
    el.innerHTML += '<div class="stat-card stat-accent"><div class="stat-label">学生数</div><div class="stat-value">' + sc + '</div><div class="stat-sub">总账号</div></div>';
    el.innerHTML += '<div class="stat-card stat-blue"><div class="stat-label">教师数</div><div class="stat-value">' + tc + '</div><div class="stat-sub">总账号</div></div>';
    el.innerHTML += '<div class="stat-card stat-green"><div class="stat-label">今日上课打卡</div><div class="stat-value">' + ckd + '/' + tot + '</div><div class="stat-sub">打卡率：' + cr + '%</div></div>';
    el.innerHTML += '<div class="stat-card stat-purple"><div class="stat-label">今日查寝打卡</div><div class="stat-value">' + dck + '/' + tot + '</div><div class="stat-sub">打卡率：' + dr + '%</div></div>';
    el.innerHTML += '<div class="stat-card stat-orange"><div class="stat-label">待审批请假</div><div class="stat-value">' + (ls.pending || 0) + '</div><div class="stat-sub">等待审批</div></div>';
    el.innerHTML += '<div class="stat-card stat-red"><div class="stat-label">待处理报修</div><div class="stat-value">' + (rs.pending || 0) + '</div><div class="stat-sub">等待处理</div></div>';
    el.innerHTML += '</div>';
    el.innerHTML += '<div class="chart-row"><div class="chart-box"><h4>请假统计</h4>';
    el.innerHTML += '<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:8px;">';
    el.innerHTML += '<div style="text-align:center;padding:12px;background:#fef3c7;border-radius:8px;"><div style="font-size:11px;color:#92400e;">待审批</div><div style="font-size:24px;font-weight:700;color:#d97706;">' + (ls.pending || 0) + '</div></div>';
    el.innerHTML += '<div style="text-align:center;padding:12px;background:#d1fae5;border-radius:8px;"><div style="font-size:11px;color:#065f46;">已通过</div><div style="font-size:24px;font-weight:700;color:#059669;">' + (ls.approved || 0) + '</div></div>';
    el.innerHTML += '<div style="text-align:center;padding:12px;background:#fee2e2;border-radius:8px;"><div style="font-size:11px;color:#991b1b;">已驳回</div><div style="font-size:24px;font-weight:700;color:#dc2626;">' + (ls.rejected || 0) + '</div></div>';
    el.innerHTML += '</div><div style="margin-top:8px;font-size:13px;color:#6b7280;">总计：<strong>' + (ls.total || 0) + '</strong></div></div>';
    el.innerHTML += '<div class="chart-box"><h4>报修统计</h4>';
    el.innerHTML += '<div style="display:grid;grid-template-columns:repeat(2,1fr);gap:8px;">';
    el.innerHTML += '<div style="text-align:center;padding:12px;background:#fef3c7;border-radius:8px;"><div style="font-size:11px;color:#92400e;">待处理</div><div style="font-size:24px;font-weight:700;color:#d97706;">' + (rs.pending || 0) + '</div></div>';
    el.innerHTML += '<div style="text-align:center;padding:12px;background:#d1fae5;border-radius:8px;"><div style="font-size:11px;color:#065f46;">已完成</div><div style="font-size:24px;font-weight:700;color:#059669;">' + (rs.done || 0) + '</div></div>';
    el.innerHTML += '</div><div style="margin-top:8px;font-size:13px;color:#6b7280;">总计：<strong>' + (rs.total || 0) + '</strong></div></div></div>';
    el.innerHTML += '<div class="section-card"><div class="card-header"><h3>今日打卡汇总</h3></div><div class="card-body"><div class="table-wrapper"><table><tr><th>类别</th><th>已打卡</th><th>未打卡</th><th>总数</th><th>打卡率</th></tr>';
    el.innerHTML += '<tr><td>上课打卡</td><td><strong>' + ckd + '</strong></td><td>' + ckn + '</td><td>' + tot + '</td><td>' + cr + '%</td></tr>';
    el.innerHTML += '<tr><td>查寝打卡</td><td><strong>' + dck + '</strong></td><td>' + (tot - dck) + '</td><td>' + tot + '</td><td>' + dr + '%</td></tr>';
    el.innerHTML += '</table></div></div></div>';
    if (as.totalCount) {
      el.innerHTML += '<div class="section-card"><div class="card-header"><h3>系统概览</h3></div><div class="card-body"><div class="detail-grid">';
      el.innerHTML += '<div class="detail-item"><div class="label">用户总数</div><div class="value">' + (as.totalCount || 0) + '</div></div>';
      el.innerHTML += '<div class="detail-item"><div class="label">活跃用户</div><div class="value">' + (as.activeCount || 0) + '</div></div>';
      el.innerHTML += '</div></div></div>';
    }
  }).catch(function(e) {
    el.innerHTML = '<div class="empty-state"><p>错误：' + html(e.message) + '</p><button class="btn-primary mt-2" onclick="renderDashboard()">重试</button></div>';
  });
}
function renderDorm() {
  var el = document.getElementById('pageContent');
  el.innerHTML = loadingHTML();
  api('/teacher/checkin/today-summary').then(function(ts) {
    var dck = (ts && ts.dormChecked) || 0;
    var tot = (ts && ts.totalStudents) || 0;
    var dkn = tot - dck;
    var rate = tot > 0 ? Math.round(dck / tot * 100) : 0;
    el.innerHTML = '<div class="stats-grid">';
    el.innerHTML += '<div class="stat-card stat-purple"><div class="stat-label">已查寝</div><div class="stat-value">' + dck + '/' + tot + '</div><div class="stat-sub">今晚</div></div>';
    el.innerHTML += '<div class="stat-card stat-orange"><div class="stat-label">未查寝</div><div class="stat-value">' + dkn + '</div><div class="stat-sub">未打卡</div></div>';
    el.innerHTML += '<div class="stat-card stat-green"><div class="stat-label">查寝率</div><div class="stat-value">' + rate + '%</div></div>';
    el.innerHTML += '</div>';
    el.innerHTML += '<div class="section-card"><div class="card-header"><h3>查寝打卡状态</h3></div><div class="card-body">';
    el.innerHTML += '<p style="color:#6b7280;font-size:13px;">今晚已有 ' + dck + ' / ' + tot + ' 名学生完成查寝打卡，' + dkn + ' 人未打卡。</p>';
    el.innerHTML += '<div style="margin-top:12px;height:32px;background:#f3f4f6;border-radius:16px;overflow:hidden;">';
    el.innerHTML += '<div style="height:100%;width:' + rate + '%;background:#8b5cf6;border-radius:16px;display:flex;align-items:center;justify-content:center;font-size:12px;color:#fff;font-weight:600;min-width:40px;">' + rate + '%</div></div></div></div>';
  }).catch(function(e) {
    el.innerHTML = '<div class="empty-state"><p>错误：' + html(e.message) + '</p></div>';
  });
}
