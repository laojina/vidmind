<template>
  <div class="app-stage">
    <div class="ambient-noise"></div>
    <div class="ambient-glow"></div>

    <header class="navbar">
      <div class="nav-content">
        <div class="brand">
          <span class="brand-do">Vid</span>
          <span class="brand-video">Mind</span>
          <span class="beta-badge">PRO</span>
        </div>

        <div class="nav-controls">
          <button v-if="!currentUser" class="auth-btn" @click="openAuthModal">
            <span class="btn-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
            </span>
            登录 / 注册
          </button>

          <div v-else class="user-profile">
            <span class="user-name">:: {{ currentUser.nickname }} ::</span>
            <button class="logout-btn" @click="logout" title="退出登录" aria-label="退出登录">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
            </button>
          </div>

          <div class="status-pill" :class="{ 'is-active': uploading }" role="status" aria-live="polite">
            <div class="status-dot"></div>
            <span class="status-text">{{ systemStatusText }}</span>
          </div>
        </div>
      </div>
    </header>

    <main class="main-container">
      <section class="hero-section">
        <h1 class="slogan-main">把视频变成结构化的知识</h1>
        <p class="slogan-sub">上传视频，AI 提取语音与画面，按你的目标生成可溯源的分析产物</p>
        <div v-if="!currentUser" class="login-hint">
          <span>尚未登录</span>
          <button type="button" @click="openAuthModal">登录后开始你的第一次视频解析 →</button>
        </div>

        <div class="upload-wrapper">
          <input
              type="file"
              id="file-input"
              @change="handleFileChange"
              accept="video/*"
              hidden
          />

          <div
              class="upload-magnet"
              :class="{ 'processing': uploading, 'is-dragover': isDragOver }"
              @dragenter.prevent="handleDragEnter"
              @dragover.prevent="isDragOver = true"
              @dragleave.prevent="handleDragLeave"
              @drop.prevent="handleDrop"
          >
            <div class="split-container" v-if="!uploading">

              <label for="file-input" class="skew-pane pane-local">
                <div class="pane-content unskew">
                  <div class="magnet-icon">
                    <svg width="42" height="42" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="17 8 12 3 7 8"></polyline><line x1="12" y1="3" x2="12" y2="15"></line></svg>
                  </div>
                  <span class="magnet-title">LOCAL FILE</span>
                  <span class="magnet-desc">{{ isDragOver ? '松手上传' : '点击 / 拖拽本地文件' }}</span>
                </div>
              </label>

              <div class="split-gap"></div>

              <div class="skew-pane pane-url">
                <div class="pane-content unskew">
                  <div class="magnet-icon">
                    <svg width="42" height="42" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="2" y1="12" x2="22" y2="12"></line><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1 4-10z"></path></svg>
                  </div>
                  <span class="magnet-title">WEB LINK</span>
                  <span class="magnet-desc">B站 / YouTube / 抖音</span>

                  <div class="url-input-box" @click.stop>
                    <input
                        v-model="videoUrl"
                        type="text"
                        inputmode="url"
                        autocomplete="off"
                        spellcheck="false"
                        placeholder="粘贴视频链接..."
                        aria-label="视频链接"
                        :disabled="uploading"
                        @keyup.enter="handleUrlUpload"
                    />
                    <button class="url-go-btn" :disabled="uploading" @click="handleUrlUpload" aria-label="解析视频链接">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"></polyline></svg>
                    </button>
                  </div>
                </div>
              </div>

            </div>

            <div class="magnet-content busy" v-else>
              <div class="quantum-loader"></div>
              <span class="busy-text">{{ uploadProgress.label }}</span>
              <span v-if="uploadProgress.filename" class="busy-file">{{ uploadProgress.filename }}</span>
              <div
                  v-if="uploadProgress.percent !== null"
                  class="upload-progress"
                  role="progressbar"
                  aria-label="视频上传进度"
                  aria-valuemin="0"
                  aria-valuemax="100"
                  :aria-valuenow="uploadProgress.percent"
              >
                <span :style="{ width: `${uploadProgress.percent}%` }"></span>
              </div>
              <span v-if="uploadProgress.detail" class="busy-stat" aria-live="polite">{{ uploadProgress.detail }}</span>
              <span v-if="uploadProgress.warning" class="busy-warning" role="status">{{ uploadProgress.warning }}</span>
              <div v-if="uploadAbort" class="busy-actions">
                <button type="button" @click="cancelUpload">取消上传</button>
              </div>
            </div>

            <div class="border-glow"></div>
          </div>

          <div v-if="resumableFile && !uploading" class="upload-resume" role="status">
            <span>{{ resumeHint }}</span>
            <button type="button" @click="resumeUpload">继续上传</button>
            <button type="button" @click="discardResumableUpload">重新开始</button>
          </div>
        </div>
        <transition name="toast-pop">
          <div
              v-if="message"
              class="notification-bar"
              :class="{ 'error': messageIsError }"
              :role="messageIsError ? 'alert' : 'status'"
              :aria-live="messageIsError ? 'assertive' : 'polite'"
              :title="messageIsError ? '点击关闭这条提示' : null"
              @click="dismissMessage"
          >
            {{ message }}
          </div>
        </transition>
      </section>

      <section v-if="list.length > 0 || currentUser" class="workspace-section">
        <div class="section-header">
          <div class="library-title">
            <h3>视频资料库</h3>
            <div class="count-chip">{{ list.length }} 个视频</div>
          </div>
          <label class="library-search">
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
            <input v-model="searchQuery" type="search" placeholder="搜索视频名称" aria-label="搜索视频名称" />
          </label>
        </div>
        <div class="card-grid">
          <div v-for="item in visibleList" :key="item.id" class="project-card">

            <button
                class="delete-btn"
                :disabled="deletingId === item.id"
                :title="deletingId === item.id ? '正在删除…' : '删除视频'"
                :aria-label="`删除 ${item.filename}`"
                @click.stop="deleteItem(item)"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"></polyline><path d="M19 6l-1 14H6L5 6"></path><path d="M8 6V4h8v2"></path>
              </svg>
            </button>
            <div class="card-meta">
              <div class="meta-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><polygon points="23 7 16 12 23 17 23 7"></polygon><rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect></svg>
              </div>
              <div class="meta-info">
                <div class="filename-mask" :title="item.filename">{{ item.filename }}</div>
                <div class="meta-tags">
                  <span class="time-tag">{{ formatTime(item.uploadTime) }}</span>
                  <span
                      class="status-indicator"
                      :class="cardStatusClass(item)"
                      :title="cardStatusTitle(item)"
                  >
                    {{ cardStatusLabel(item) }}
                  </span>
                </div>
              </div>
            </div>

            <div class="action-dock">
              <button
                  class="dock-item"
                  :disabled="item.status !== 'COMPLETED'"
                  :title="actionTitle(item, '下载音频')"
                  @click="downloadAudio(item)"
              >
                <span class="item-icon">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18V5l12-2v13"></path><circle cx="6" cy="18" r="3"></circle><circle cx="18" cy="16" r="3"></circle></svg>
                </span>
                <span class="item-label">下载音频</span>
              </button>

              <button
                  class="dock-item"
                  :disabled="item.status !== 'COMPLETED'"
                  :title="actionTitle(item, '提取文字')"
                  @click="transcribe(item.id)"
              >
                <span class="item-icon">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
                </span>
                <span class="item-label">提取文字</span>
              </button>

              <button
                  class="dock-item ai-core"
                  :disabled="item.status !== 'COMPLETED'"
                  :title="actionTitle(item, '打开 Video Agent')"
                  @click="openAgent(item)"
              >
                <span class="item-icon">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="4" width="16" height="16" rx="2" ry="2"></rect><rect x="9" y="9" width="6" height="6"></rect><line x1="9" y1="1" x2="9" y2="4"></line><line x1="15" y1="1" x2="15" y2="4"></line><line x1="9" y1="20" x2="9" y2="23"></line><line x1="15" y1="20" x2="15" y2="23"></line><line x1="20" y1="9" x2="23" y2="9"></line><line x1="20" y1="14" x2="23" y2="14"></line><line x1="1" y1="9" x2="4" y2="9"></line><line x1="1" y1="14" x2="4" y2="14"></line></svg>
                </span>
                <div class="label-group">
                  <span class="item-label">Video Agent</span>
                </div>
                <div class="shimmer"></div>
              </button>
            </div>
          </div>
        </div>
        <div v-if="visibleList.length === 0" class="library-empty">
          <p>没有找到“{{ searchQuery }}”</p>
          <button type="button" @click="searchQuery = ''">清除搜索</button>
        </div>
      </section>

      <div class="sidebar-backdrop" v-if="sidebar.visible" @click="closeSidebar"></div>
      <div
          ref="sidebarPanel"
          class="sidebar-panel"
          :class="{ 'is-open': sidebar.visible }"
          :inert="!sidebar.visible"
          role="dialog"
          aria-modal="true"
          tabindex="-1"
          :aria-label="sidebar.title || '任务详情'"
      >
        <div class="sidebar-header">
          <div class="sidebar-title">
            <span class="icon" v-if="sidebar.type === 'ai'">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12h2"></path><path d="M20 12h2"></path><path d="M12 2v2"></path><path d="M12 20v2"></path><path d="M20.2 6.47l-1.4 1.4"></path><path d="M15.9 5.35l-1.4-1.4"></path><path d="M9 11a3 3 0 1 0 6 0a3 3 0 0 0-6 0"></path></svg>
            </span>
            <span class="icon" v-else>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
            </span>
            {{ sidebar.title }}
          </div>
          <button class="close-btn" @click="closeSidebar" aria-label="关闭分析面板">×</button>
        </div>
        <div ref="sidebarBody" class="sidebar-body">
          <div v-if="sidebar.type === 'ai'" class="video-evidence">
            <video
                v-if="sidebar.playbackUrl"
                ref="videoPlayer"
                :src="sidebar.playbackUrl"
                controls
                playsinline
                preload="metadata"
                @error="handlePlaybackError"
            ></video>
            <div v-else-if="sidebar.playbackLoading" class="video-evidence-loading">正在载入原视频...</div>
            <div v-else-if="sidebar.playbackError" class="video-evidence-error" role="alert">
              <span>{{ sidebar.playbackError }}</span>
              <button type="button" @click="retryPlayback">重新加载</button>
            </div>
            <p v-if="sidebar.playbackUrl">点击分析结果中的时间戳，可跳转到对应画面</p>
          </div>
          <div v-if="sidebar.type === 'ai' && sidebar.mode === 'compose'" class="agent-composer">
            <p class="agent-caption">选择分析模式（决定产物形态）</p>
            <div class="goal-presets agent-mode-row">
              <button
                  v-for="m in analysisModes"
                  :key="m.value"
                  :class="{ active: sidebar.analysisMode === m.value }"
                  @click="sidebar.analysisMode = m.value"
              >
                <strong>{{ m.title }}</strong>
                <span>{{ m.description }}</span>
              </button>
            </div>
            <p class="agent-caption">告诉 Agent 你希望从视频中得到什么产物</p>
            <p v-if="sidebar.error" class="inline-error" role="alert">{{ sidebar.error }}</p>
            <textarea
                v-model="sidebar.goal"
                maxlength="500"
                placeholder="例如：梳理核心观点，给出带时间戳的证据和可执行建议（Ctrl / ⌘ + Enter 提交）"
                @keydown.ctrl.enter.prevent="submitAgent"
                @keydown.meta.enter.prevent="submitAgent"
            ></textarea>
            <p v-if="sidebar.goal.length > 400" class="field-counter">已输入 {{ sidebar.goal.length }} / 500 字</p>
            <div class="goal-presets">
              <button
                  v-for="preset in goalPresets"
                  :key="preset.title"
                  :class="{ active: sidebar.goal === preset.prompt }"
                  @click="sidebar.goal = preset.prompt"
              >
                <strong>{{ preset.title }}</strong>
                <span>{{ preset.description }}</span>
              </button>
            </div>
            <button class="agent-run-btn" :disabled="!sidebar.goal.trim()" @click="submitAgent">
              {{ sidebar.error ? '重新分析' : '开始分析' }}
            </button>
          </div>

          <div v-else-if="sidebar.loading" class="agent-running">
            <div class="loading-state">
              <div class="quantum-loader small"></div>
              <p aria-live="polite">{{ loadingHeadline }}</p>
              <p v-if="sidebar.streamOffline" class="stream-offline" role="status">
                连接中断，正在自动重连（第 {{ sidebar.streamRetry }} 次）· 任务仍在服务端继续
              </p>
              <p class="loading-hint">可以关闭本面板，任务会在后台继续，完成后会通知你</p>
            </div>
            <div v-if="sidebar.plan?.tasks?.length" class="agent-meta-block">
              <span class="meta-label">任务计划</span>
              <ol><li v-for="task in sidebar.plan.tasks" :key="task">{{ task }}</li></ol>
            </div>
            <div v-if="traceStages.length" class="agent-meta-block">
              <span class="meta-label">已完成阶段</span>
              <div class="stage-list"><span v-for="stage in traceStages" :key="stage[0]">{{ stage[0] }} · {{ stage[1] }}</span></div>
            </div>
          </div>

          <div v-else>
            <div v-if="sidebar.type === 'ai'">
              <div class="result-actions">
                <button type="button" @click="startNewAnalysis">更换产物</button>
                <button type="button" :disabled="!sidebar.content" @click="copyResult">复制结果</button>
                <button type="button" :disabled="!sidebar.content" @click="downloadResult">导出 Markdown</button>
              </div>
              <div class="evidence-search">
                <div class="evidence-search-form">
                  <input
                      v-model="sidebar.evidenceQuery"
                      aria-label="视频证据检索"
                      maxlength="500"
                      placeholder="定位 PPT、字幕、代码或某段讲解"
                      @keyup.enter="searchEvidence"
                  />
                  <button type="button" :disabled="sidebar.evidenceLoading || !sidebar.evidenceQuery.trim()" @click="searchEvidence">
                    {{ sidebar.evidenceLoading ? '检索中' : '定位证据' }}
                  </button>
                </div>
                <p v-if="sidebar.evidenceError" class="evidence-search-error" aria-live="polite">{{ sidebar.evidenceError }}</p>
                <div v-if="sidebar.evidenceResults.length" class="evidence-search-results" aria-live="polite">
                  <button
                      v-for="hit in sidebar.evidenceResults"
                      :key="`${hit.startMs}-${hit.endMs}`"
                      type="button"
                      :title="hit.snippet || '该时间段暂无可展示文本'"
                      @click="seekToEvidence(hit.startMs)"
                  >
                    <strong>{{ formatEvidenceTime(hit.startMs) }}</strong>
                    <small>{{ hit.source || '视频证据' }}</small>
                    <span>{{ hit.snippet || '该时间段暂无可展示文本' }}</span>
                  </button>
                </div>
              </div>
              <div class="markdown-content" v-html="renderedMarkdown" @click="seekEvidence"></div>
              <details v-if="sidebar.plan?.tasks?.length || traceStages.length" class="agent-inspector">
                <summary>分析详情</summary>
                <div class="agent-inspector-content">
                <div v-if="sidebar.plan?.tasks?.length" class="agent-meta-block">
                  <span class="meta-label">Planner 任务</span>
                  <div v-if="sidebar.editingPlan" class="plan-editor">
                    <div v-for="(_, index) in sidebar.planDraft" :key="index" class="plan-editor-row">
                      <input v-model="sidebar.planDraft[index]" maxlength="500" :aria-label="`任务 ${index + 1}`" />
                      <button type="button" title="删除任务" @click="removePlanTask(index)">×</button>
                    </div>
                    <button v-if="sidebar.planDraft.length < 5" type="button" @click="addPlanTask">添加任务</button>
                    <div class="plan-editor-actions">
                      <button type="button" @click="cancelPlanEdit">取消</button>
                      <button type="button" :disabled="sidebar.rerunLoading" @click="rerunWithPlan">
                        {{ sidebar.rerunLoading ? '提交中' : '按新计划重跑' }}
                      </button>
                    </div>
                  </div>
                  <template v-else>
                    <ol><li v-for="task in sidebar.plan.tasks" :key="task">{{ task }}</li></ol>
                    <button type="button" class="plan-edit-trigger" @click="startPlanEdit">调整计划</button>
                  </template>
                </div>
                <div v-if="traceStages.length" class="agent-meta-block">
                  <span class="meta-label">执行轨迹</span>
                  <div class="stage-list"><span v-for="stage in traceStages" :key="stage[0]">{{ stage[0] }} · {{ stage[1] }}</span></div>
                </div>
                <div v-if="sidebar.evaluation && Object.keys(sidebar.evaluation).length" class="quality-row">
                  <span>结构完整 {{ sidebar.evaluation.structuredValid ? '通过' : '待完善' }}</span>
                  <span>证据支持 {{ formatPercent(sidebar.evaluation.evidenceSupportRate) }}</span>
                  <span>Critic {{ sidebar.evaluation.criticPassed ? '通过' : '达到轮次上限' }}</span>
                </div>
                </div>
              </details>
              <div class="follow-up-box">
                <textarea
                    v-model="sidebar.followUp"
                    maxlength="500"
                    placeholder="基于视频继续追问...（Ctrl / ⌘ + Enter 发送）"
                    @keydown.ctrl.enter.prevent="submitFollowUp"
                    @keydown.meta.enter.prevent="submitFollowUp"
                ></textarea>
                <button :disabled="sidebar.followUpLoading || !sidebar.followUp.trim()" @click="submitFollowUp">
                  {{ sidebar.followUpLoading ? '分析中' : '追问' }}
                </button>
              </div>
              <div class="feedback-row">
                <span>这个结果有帮助吗？</span>
                <button :disabled="sidebar.feedbackLoading" :class="{ active: sidebar.feedback === 1 }" :aria-pressed="sidebar.feedback === 1" @click="sendFeedback(1)" title="有帮助">赞</button>
                <button :disabled="sidebar.feedbackLoading" :class="{ active: sidebar.feedback === -1 }" :aria-pressed="sidebar.feedback === -1" @click="sendFeedback(-1)" title="需改进">踩</button>
              </div>
            </div>
            <div v-else class="text-content">
              <p v-if="sidebar.error" class="inline-error" role="alert">{{ sidebar.error }}</p>
              <template v-if="sidebar.content">
                <div class="result-actions">
                  <button type="button" @click="copyResult">复制全文</button>
                  <button type="button" @click="downloadResult">导出文本</button>
                </div>
                <p class="text-meta">{{ transcriptMeta }}</p>
                <pre>{{ sidebar.content }}</pre>
              </template>
              <p v-else-if="!sidebar.error" class="text-meta">这个视频还没有可展示的转写文本。</p>
            </div>
          </div>
        </div>
      </div>

      <div v-if="showAuthModal" class="auth-backdrop" @click.self="closeAuthModal">
        <div
            ref="authPanel"
            class="auth-panel"
            role="dialog"
            aria-modal="true"
            aria-labelledby="auth-title"
            @keydown="trapAuthFocus"
        >
          <div class="auth-header">
            <h2 id="auth-title" class="auth-title">{{ authMode === 'login' ? '用户登录' : '新用户注册' }}</h2>
            <button class="close-btn" @click="closeAuthModal" aria-label="关闭登录窗口">×</button>
          </div>
          <form class="auth-body" @submit.prevent="handleAuth">
            <div class="input-group">
              <label for="auth-username">用户名</label>
              <input id="auth-username" v-model="authForm.username" type="text" placeholder="输入账号" autocomplete="username" autofocus />
            </div>
            <div class="input-group">
              <label for="auth-password">密码</label>
              <input id="auth-password" v-model="authForm.password" type="password" placeholder="输入密码" :autocomplete="authMode === 'login' ? 'current-password' : 'new-password'" />
            </div>
            <div class="input-group" v-if="authMode === 'register'">
              <label for="auth-nickname">昵称</label>
              <input id="auth-nickname" v-model="authForm.nickname" type="text" placeholder="设置一个好听的名字" autocomplete="nickname" />
            </div>
            <div class="auth-action">
              <button type="submit" class="cyber-btn" :disabled="authLoading">
                <span v-if="!authLoading">{{ authMode === 'login' ? '立即登录' : '提交注册' }}</span>
                <span v-else>请求处理中...</span>
              </button>
            </div>
            <div class="auth-toggle">
              <span class="toggle-text">{{ authMode === 'login' ? '没有账号?' : '已有账号?' }}</span>
              <button type="button" class="toggle-link" @click="switchAuthMode()">{{ authMode === 'login' ? '去注册' : '去登录' }}</button>
            </div>
            <p
                v-if="authMessage"
                class="auth-msg"
                :class="{'error': authError}"
                :role="authError ? 'alert' : 'status'"
                aria-live="polite"
            >{{ authMessage }}</p>
          </form>
        </div>
      </div>
      <footer class="app-footer">
        <span>VidMind</span>
        <span class="footer-dot">·</span>
        <span>目标驱动的视频解析工作台</span>
        <span class="footer-dot">·</span>
        <span>Spring Boot 3 / Vue 3 / RocketMQ / Qdrant</span>
      </footer>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch, onMounted, onUnmounted } from 'vue'
import { apiRequest, clearAuthToken, hasAuthToken, setAuthToken } from './api'
import {
  forgetUploadProgress,
  formatBytes,
  formatDurationText,
  hasUploadProgress,
  uploadVideoInChunks,
  validateVideoFile
} from './chunkUpload'
import { DEMO_ITEM } from './demoData'
import { createTaskStreams } from './taskEvents'
import { useAnalysisWorkspace } from './useAnalysisWorkspace'

// --- 变量定义 ---
const DEMO_MODE = new URLSearchParams(window.location.search).has('demo')
const MESSAGE_TIMEOUT_MS = 4000
const file = ref(null)
const videoUrl = ref('')
const message = ref('')
const messageIsError = ref(false)
const uploading = ref(false)
const uploadProgress = ref({ label: '准备上传', filename: '', percent: null, detail: '', warning: '' })
const uploadAbort = ref(null)
const resumableFile = ref(null)
const resumableChunks = ref({ done: 0, total: 0 })
const list = ref([])
const searchQuery = ref('')
const videoPlayer = ref(null)
const sidebarPanel = ref(null)
const sidebarBody = ref(null)
const authPanel = ref(null)
const deletingId = ref(null)
const isOffline = ref(typeof navigator !== 'undefined' && navigator.onLine === false)
const activeTasks = ref([])
const elapsedSeconds = ref(0)
const visibleList = computed(() => {
  const query = searchQuery.value.trim().toLocaleLowerCase()
  if (!query) return list.value
  return list.value.filter(item => item.filename?.toLocaleLowerCase().includes(query))
})
const isDragOver = ref(false)
const currentUser = ref(null)
const showAuthModal = ref(false)
const authMode = ref('login')
const authLoading = ref(false)
const authMessage = ref('')
const authError = ref(false)
const authForm = ref({ username: '', password: '', nickname: '' })
const taskStreams = createTaskStreams({
  onActiveChange: tasks => { activeTasks.value = tasks }
})
const VIDEO_EXTENSIONS = new Set(['mp4', 'mov', 'mkv', 'avi', 'webm', 'm4v'])
let dragDepth = 0
let messageTimer = null
let elapsedTimer = null
let lastUploadProgress = {}
let focusBeforeAuth = null
let focusBeforeSidebar = null

// --- 状态展示 ---
const activeTaskOf = mediaId => activeTasks.value.find(task => String(task.id) === String(mediaId))

const systemStatusText = computed(() => {
  if (isOffline.value) return '网络已断开'
  if (uploading.value) {
    return uploadProgress.value.percent !== null
      ? `上传 ${uploadProgress.value.percent}%`
      : '处理中'
  }
  if (activeTasks.value.length) return `后台任务 ${activeTasks.value.length}`
  return '系统就绪'
})

const cardStatusClass = item => {
  if (activeTaskOf(item.id)) return 'processing'
  return mediaStatusClass(item.status)
}
const cardStatusLabel = item => {
  const task = activeTaskOf(item.id)
  if (task) return task.type === 'ai' ? 'ANALYZING' : 'TRANSCRIBING'
  return mediaStatusLabel(item.status)
}
const cardStatusTitle = item => {
  const task = activeTaskOf(item.id)
  if (!task) return null
  return task.type === 'ai'
    ? 'AI 分析正在后台执行，完成后会提示你'
    : '文字提取正在后台执行，完成后会提示你'
}
const actionTitle = (item, label) => item.status === 'COMPLETED'
  ? null
  : `视频尚未处理完成，暂时无法${label}`

const elapsedLabel = computed(() => {
  const total = elapsedSeconds.value
  if (total < 1) return ''
  const minutes = String(Math.floor(total / 60)).padStart(2, '0')
  return `${minutes}:${String(total % 60).padStart(2, '0')}`
})

const loadingHeadline = computed(() => {
  const fallback = sidebar.value.type === 'ai'
    ? 'Agent 正在分析视频证据'
    : '正在识别视频语音'
  const headline = sidebar.value.statusMessage || fallback
  // 用“已等待”而不是“已运行”：接管历史任务时计时是从打开面板算起的。
  return elapsedLabel.value ? `${headline} · 已等待 ${elapsedLabel.value}` : headline
})

const transcriptMeta = computed(() => {
  const length = sidebar.value.content?.length || 0
  if (!length) return ''
  return `共 ${length.toLocaleString('zh-CN')} 字`
})

const resumeHint = computed(() => {
  const target = resumableFile.value
  if (!target) return ''
  const { done, total } = resumableChunks.value
  const progress = total ? `已完成 ${Math.round((done / total) * 100)}%` : '已保留上传进度'
  return `${target.name} ${progress}，可继续未完成的上传`
})

// --- 核心业务逻辑 ---

const handleDragEnter = () => {
  dragDepth += 1
  isDragOver.value = true
}

// 拖过子元素也会触发 dragleave，用进出计数避免提示文案反复闪烁。
const handleDragLeave = () => {
  dragDepth = Math.max(0, dragDepth - 1)
  if (!dragDepth) isDragOver.value = false
}

const resetDragState = () => {
  dragDepth = 0
  isDragOver.value = false
}

/** 统一入口：登录、格式、体积三道校验全部在进入上传态之前完成。 */
const startUpload = async (selectedFile, extraFileCount = 0) => {
  if (uploading.value) {
    showMsg('已有上传任务在进行，请等当前任务结束', true)
    return
  }
  if (!currentUser.value) {
    showMsg('⚠️ 权限受限：请先登录系统', true)
    openAuthModal()
    return
  }
  if (!selectedFile) return
  if (!isSupportedVideo(selectedFile)) {
    showMsg(`⚠️ ${selectedFile.name} 不是受支持的视频格式`, true)
    return
  }
  const invalid = validateVideoFile(selectedFile)
  if (invalid) {
    showMsg(`⚠️ ${invalid}`, true)
    return
  }
  if (extraFileCount > 0) {
    showMsg(`一次只处理一个视频，已选择 ${selectedFile.name}，其余 ${extraFileCount} 个已忽略`)
  }
  file.value = selectedFile
  videoUrl.value = ''
  await uploadFile()
}

const handleFileChange = async (e) => {
  const selected = e.target.files
  await startUpload(selected?.[0], Math.max(0, (selected?.length || 0) - 1))
  e.target.value = ''
}

const handleDrop = async (e) => {
  resetDragState()
  const dropped = e.dataTransfer?.files
  if (!dropped?.length) return
  await startUpload(dropped[0], dropped.length - 1)
}

const buildUploadWarning = progress => {
  if (progress.retryingCount) {
    return `网络不稳定，正在重试 ${progress.retryingCount} 个分片（第 ${progress.retryAttempt}/${progress.retryMaxAttempts} 次）`
  }
  if (progress.resumedChunks) {
    return `已续传：跳过 ${progress.resumedChunks} 个此前完成的分片`
  }
  return ''
}

const applyUploadProgress = progress => {
  lastUploadProgress = progress
  const merging = progress.phase === 'merging'
  const detail = [`${formatBytes(progress.uploadedBytes)} / ${formatBytes(progress.totalBytes)}`]
  detail.push(`分片 ${progress.completedChunks}/${progress.totalChunks}`)
  if (!merging && progress.bytesPerSecond) {
    detail.push(`${formatBytes(progress.bytesPerSecond)}/s`)
    const eta = formatDurationText(progress.etaSeconds)
    if (eta) detail.push(`剩余约 ${eta}`)
  }
  uploadProgress.value = {
    label: merging ? '分片已全部送达，正在服务端合并' : '正在安全上传',
    filename: file.value?.name || uploadProgress.value.filename,
    percent: progress.percent,
    detail: detail.join(' · '),
    warning: buildUploadWarning(progress)
  }
}

const rememberResumableUpload = target => {
  if (!target || !hasUploadProgress(target)) {
    resumableFile.value = null
    return
  }
  resumableFile.value = target
  resumableChunks.value = {
    done: lastUploadProgress.completedChunks || 0,
    total: lastUploadProgress.totalChunks || 0
  }
}

const uploadFile = async () => {
  const target = file.value
  if (!target) return
  if (DEMO_MODE) {
    showMsg('演示模式：已模拟完成分片上传')
    return
  }

  const controller = new AbortController()
  uploadAbort.value = controller
  uploading.value = true
  resumableFile.value = null
  lastUploadProgress = {}
  const uploadUserId = currentUser.value?.id
  uploadProgress.value = {
    label: hasUploadProgress(target) ? '正在核对已上传分片' : '准备分片上传',
    filename: target.name,
    percent: 0,
    detail: `0 B / ${formatBytes(target.size)}`,
    warning: ''
  }

  try {
    const uploadedMedia = await uploadVideoInChunks(target, applyUploadProgress, controller.signal)
    if (currentUser.value?.id !== uploadUserId) return
    resumableFile.value = null
    showMsg(`✅ ${target.name} 上传完成`)
    await fetchList({ notify: true })
    openAgent(uploadedMedia)
  } catch (error) {
    if (currentUser.value?.id !== uploadUserId) return
    rememberResumableUpload(target)
    if (error?.aborted) {
      showMsg('上传已取消，进度已保留，可点“继续上传”接着传')
      return
    }
    console.error(error)
    showMsg(
      resumableFile.value
        ? `❌ 上传中断：${error.message}（进度已保留，可继续上传）`
        : `❌ 上传失败：${error.message}`,
      true
    )
  } finally {
    uploading.value = false
    uploadAbort.value = null
    file.value = null
  }
}

const cancelUpload = () => {
  if (!uploadAbort.value) return
  uploadProgress.value = { ...uploadProgress.value, label: '正在取消上传', warning: '' }
  uploadAbort.value.abort()
}

const resumeUpload = async () => {
  const target = resumableFile.value
  if (!target || uploading.value) return
  file.value = target
  await uploadFile()
}

const discardResumableUpload = () => {
  forgetUploadProgress(resumableFile.value)
  resumableFile.value = null
  resumableChunks.value = { done: 0, total: 0 }
  showMsg('已清除保留的上传进度，下次将从头开始')
}

const handleUrlUpload = async () => {
  // 支持直接粘贴抖音/B站分享文本：从整段文字中自动抽出第一个 http/https 链接
  let normalizedUrl = videoUrl.value.trim()
  if (normalizedUrl && !/^https?:\/\//i.test(normalizedUrl)) {
    const matched = normalizedUrl.match(/https?:\/\/[A-Za-z0-9\-._~:\/?#\[\]@!$&'()*+,;=%]+/i)
    if (matched) {
      normalizedUrl = matched[0].replace(/[.,;:!）]+$/, '')
      videoUrl.value = normalizedUrl
    }
  }
  if (!normalizedUrl) return
  if (uploading.value) {
    showMsg('已有上传任务在进行，请等当前任务结束', true)
    return
  }
  if (DEMO_MODE) {
    videoUrl.value = ''
    showMsg('演示模式：已模拟完成链接解析')
    return
  }

  if (!currentUser.value) {
    showMsg('⚠️ 权限受限：请先登录系统', true)
    openAuthModal()
    return
  }

  let parsedUrl
  try {
    parsedUrl = new URL(normalizedUrl)
  } catch {
    parsedUrl = null
  }
  if (!parsedUrl || !['http:', 'https:'].includes(parsedUrl.protocol)) {
    showMsg('⚠️ 请输入合法的 http/https 链接', true)
    return
  }

  uploading.value = true
  const uploadUserId = currentUser.value?.id
  uploadProgress.value = {
    label: '正在解析视频链接',
    filename: parsedUrl.hostname,
    percent: null,
    detail: '服务端正在拉取源视频，时长取决于源站速度',
    warning: ''
  }
  messageIsError.value = false
  message.value = '正在解析链接并极速下载 (低码率模式)...'

  const formData = new FormData()
  formData.append('url', normalizedUrl)

  try {
    const res = await apiRequest('/media/upload-url', {
      method: 'POST',
      body: formData
    })
    if (!res.ok) throw new Error(await res.text())
    const uploadedMedia = await res.json()
    if (currentUser.value?.id !== uploadUserId) return

    showMsg('✅ 链接资源已入库')
    videoUrl.value = ''
    await fetchList({ notify: true })
    openAgent(uploadedMedia)
  } catch (error) {
    console.error(error)
    if (currentUser.value?.id !== uploadUserId) return
    let errMsg = error.message
    if (errMsg.includes("Unsupported URL")) errMsg = "不支持该平台链接"
    showMsg('❌ 解析失败: ' + errMsg, true)
  } finally {
    uploading.value = false
  }
}

/** 成功提示自动消失；错误提示保留到用户点掉，避免关键失败原因 4 秒后就没了。 */
const showMsg = (msg, isError = false) => {
  clearTimeout(messageTimer)
  messageTimer = null
  message.value = msg
  messageIsError.value = isError
  if (isError) return
  messageTimer = setTimeout(() => {
    if (message.value !== msg) return
    message.value = ''
    messageIsError.value = false
  }, MESSAGE_TIMEOUT_MS)
}

const dismissMessage = () => {
  if (!messageIsError.value) return
  clearTimeout(messageTimer)
  messageTimer = null
  message.value = ''
  messageIsError.value = false
}

const fetchList = async ({ notify = false } = {}) => {
  if (DEMO_MODE) return list.value
  if (!currentUser.value) {
    list.value = []
    return list.value
  }
  try {
    // 带时间戳绕开浏览器缓存，避免删除/新增之后列表还是旧的。
    const res = await apiRequest(`/media/list?_t=${Date.now()}`)
    if (res.status === 401) return null
    if (!res.ok) throw new Error('加载视频列表失败')
    list.value = await res.json()
  } catch (error) {
    console.error(error)
    if (notify) showMsg('视频资料库加载失败，请稍后刷新', true)
    return null
  }
  return list.value
}

const isSupportedVideo = selectedFile => {
  if (selectedFile.type?.startsWith('video/')) return true
  const extension = selectedFile.name?.split('.').pop()?.toLowerCase()
  return VIDEO_EXTENSIONS.has(extension)
}

const mediaStatusClass = status => ['COMPLETED', 'PROCESSING', 'FAILED'].includes(status)
  ? status.toLowerCase()
  : 'unknown'
const mediaStatusLabel = status => ({
  COMPLETED: 'READY',
  PROCESSING: 'PROCESSING',
  FAILED: 'FAILED'
})[status] || 'PENDING'

const {
  sidebar,
  goalPresets,
  analysisModes,
  traceStages,
  renderedMarkdown,
  transcribe,
  closeSidebar,
  openAgent,
  submitAgent,
  startNewAnalysis,
  showDemoResult,
  startPlanEdit,
  cancelPlanEdit,
  addPlanTask,
  removePlanTask,
  rerunWithPlan,
  submitFollowUp,
  searchEvidence,
  sendFeedback,
  retryPlayback,
  handlePlaybackError,
  resetWorkspace,
  discardMediaWorkspace,
  formatPercent
} = useAnalysisWorkspace({
  demoMode: DEMO_MODE,
  taskStreams,
  showMessage: showMsg,
  refreshMediaList: fetchList,
  findMediaItem: id => list.value.find(item => item.id === id),
  onAnswerAppended: () => scrollToLatestAnswer()
})

/** 追问的答案追加在长文末尾，主动滚过去，否则用户会以为“点了没反应”。 */
const scrollToLatestAnswer = async () => {
  await nextTick()
  const container = sidebarBody.value?.querySelector('.markdown-content')
  if (!container) return
  const headings = container.querySelectorAll('h2, h3')
  const anchor = headings.length ? headings[headings.length - 1] : container.lastElementChild
  anchor?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const seekVideo = seconds => {
  if (!Number.isFinite(seconds)) return
  const player = videoPlayer.value
  if (!player) {
    if (sidebar.value.playbackError) {
      showMsg('原视频加载失败，无法跳转，可先点“重新加载”', true)
    } else if (sidebar.value.playbackLoading) {
      showMsg('原视频还在载入，稍等一下再点这个时间戳')
    } else {
      showMsg('这个视频暂时没有可播放的原片，无法跳转', true)
    }
    return
  }
  if (player.readyState === 0) {
    player.addEventListener('loadedmetadata', () => seekVideo(seconds), { once: true })
    return
  }
  const duration = player.duration
  const maxTime = Number.isFinite(duration) ? Math.max(0, duration - 0.1) : Number.MAX_SAFE_INTEGER
  player.currentTime = Math.min(Math.max(0, seconds), maxTime)
  player.play().catch(() => {})
  player.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
}

const seekEvidence = event => {
  const link = event.target.closest('a[href^="#video-t="]')
  if (!link) return
  event.preventDefault()
  seekVideo(Number(link.getAttribute('href').split('=')[1]))
}

const seekToEvidence = timestampMs => seekVideo(Number(timestampMs) / 1000)
const formatEvidenceTime = timestampMs => {
  const seconds = Math.max(0, Math.floor(Number(timestampMs) / 1000))
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const time = `${String(minutes).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`
  return hours ? `${String(hours).padStart(2, '0')}:${time}` : time
}

/** Clipboard API 在非 HTTPS 环境不可用，这里保留一条降级路径，避免“复制失败”变成死路。 */
const copyToClipboard = async text => {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch {
    // 继续走下面的降级方案。
  }
  try {
    const scratch = document.createElement('textarea')
    scratch.value = text
    scratch.setAttribute('readonly', '')
    scratch.style.position = 'fixed'
    scratch.style.top = '0'
    scratch.style.opacity = '0'
    document.body.appendChild(scratch)
    scratch.select()
    const copied = document.execCommand('copy')
    document.body.removeChild(scratch)
    return copied
  } catch {
    return false
  }
}

const copyResult = async () => {
  const content = sidebar.value.content
  if (!content) {
    showMsg('还没有可复制的内容', true)
    return
  }
  const label = sidebar.value.type === 'ai' ? '分析结果' : '转写全文'
  if (await copyToClipboard(content)) showMsg(`${label}已复制`)
  else showMsg('复制失败，请手动选中内容后复制', true)
}

const resultFileBaseName = () => {
  const title = sidebar.value.title || ''
  const raw = title.split(' · ').slice(1).join(' · ') || title
  const cleaned = raw.replace(/\.[^/.]+$/, '').replace(/[\\/:*?"<>|]/g, '_').trim()
  return cleaned || (sidebar.value.type === 'ai' ? 'analysis' : 'transcript')
}

const downloadResult = () => {
  const content = sidebar.value.content
  if (!content) {
    showMsg('还没有可导出的内容', true)
    return
  }
  const isMarkdown = sidebar.value.type === 'ai'
  const blob = new Blob([content], {
    type: isMarkdown ? 'text/markdown;charset=utf-8' : 'text/plain;charset=utf-8'
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${resultFileBaseName()}.${isMarkdown ? 'md' : 'txt'}`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  // 立刻 revoke 在部分浏览器会导致下载拿不到内容，延后一拍更稳。
  setTimeout(() => URL.revokeObjectURL(url), 0)
  showMsg(`已导出 ${link.download}`)
}

const deleteItem = async (item) => {
  if (DEMO_MODE) {
    list.value = list.value.filter(i => i.id !== item.id)
    discardMediaWorkspace(item.id)
    showMsg('演示任务已移除')
    return
  }
  if (deletingId.value) return
  const runningTask = activeTaskOf(item.id)
  const warning = runningTask
    ? '\n\n注意：该视频还有任务正在后台执行，删除后这次的结果会丢失。'
    : ''
  if (!confirm(`确认要永久删除 "${item.filename}" 吗？${warning}`)) return
  deletingId.value = item.id
  try {
    const res = await apiRequest(`/media/delete?id=${item.id}`, { method: 'DELETE' })
    const text = await res.text()
    if (res.ok) {
      showMsg(`已删除 ${item.filename}`)
      list.value = list.value.filter(i => i.id !== item.id)
      discardMediaWorkspace(item.id)
    } else {
      showMsg('❌ ' + text, true)
    }
  } catch (e) {
    showMsg('❌ 删除请求失败', true)
  } finally {
    deletingId.value = null
  }
}

const formatTime = (timeStr) => {
  if (!timeStr) return '--'
  const date = new Date(timeStr)
  if (Number.isNaN(date.getTime())) return '--'
  return `${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const downloadAudio = async (item) => {
  if (DEMO_MODE) {
    showMsg(`演示模式：${item.filename} 音频已准备`)
    return
  }
  let fileName = item.filename || 'audio.mp3';
  fileName = fileName.replace(/\.[^/.]+$/, "") + ".mp3";
  try {
    showMsg('正在转码并下载...')
    const res = await apiRequest(`/analysis/download?id=${item.id}`)
    // 失败时后端返回的是 JSON 信封，api.js 会把 message 解包给 text()，
    // 这里读出来向上抛，避免把“视频不存在 / 无权访问 / 转码失败”统一显示成同一句话。
    if (!res.ok) throw new Error((await res.text()) || '请稍后重试')
    const blob = await res.blob()
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)
    showMsg('✅ 下载完成')
  } catch (e) {
    showMsg('音频下载失败：' + (e?.message || '请稍后重试'), true)
  }
}

const restoreFocus = element => {
  if (element?.isConnected && typeof element.focus === 'function') element.focus()
}

const openAuthModal = () => {
  if (showAuthModal.value) return
  focusBeforeAuth = document.activeElement
  showAuthModal.value = true
  authMessage.value = ''
  authForm.value = { username: '', password: '', nickname: '' }
}
const closeAuthModal = () => {
  showAuthModal.value = false
  restoreFocus(focusBeforeAuth)
  focusBeforeAuth = null
}
const closeActiveOverlay = () => {
  if (showAuthModal.value) closeAuthModal()
  else if (sidebar.value.visible) closeSidebar()
}
const handleKeydown = event => {
  if (event.key === 'Escape') closeActiveOverlay()
}

/** 弹窗内循环 Tab，键盘用户不会一路跳到被遮住的背景里。 */
const trapAuthFocus = event => {
  if (event.key !== 'Tab' || !authPanel.value) return
  const focusable = [...authPanel.value.querySelectorAll('button, input, [tabindex]:not([tabindex="-1"])')]
    .filter(element => !element.disabled && element.offsetParent !== null)
  if (!focusable.length) return
  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  const active = document.activeElement
  if (event.shiftKey && (active === first || !authPanel.value.contains(active))) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && active === last) {
    event.preventDefault()
    first.focus()
  }
}

const switchAuthMode = ({ keepMessage = false } = {}) => {
  authMode.value = authMode.value === 'login' ? 'register' : 'login'
  if (!keepMessage) authMessage.value = ''
}
const handleAuth = async () => {
  if (!authForm.value.username || !authForm.value.password) {
    authMessage.value = '请输入完整的账号和密码'
    authError.value = true
    return
  }
  authLoading.value = true
  authMessage.value = ''
  const endpoint = authMode.value === 'login' ? '/user/login' : '/user/register'
  try {
    const res = await apiRequest(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(authForm.value)
    })
    if (!res.ok) {
      authMessage.value = (await res.text()) || `请求失败（HTTP ${res.status}）`
      authError.value = true
      return
    }
    const data = await res.json().catch(() => null)
    if (!data?.userInfo) {
      authMessage.value = '服务端返回异常，请稍后重试'
      authError.value = true
      return
    }
    if (authMode.value === 'login') {
      currentUser.value = data.userInfo
      localStorage.setItem('user', JSON.stringify(data.userInfo))
      setAuthToken(data.token)
      closeAuthModal()
      showMsg(`欢迎回来，${data.userInfo.nickname}`)
      fetchList({ notify: true })
    } else {
      authMessage.value = '注册成功，账号密码已保留，直接点“立即登录”即可'
      authError.value = false
      setTimeout(() => switchAuthMode({ keepMessage: true }), 900)
    }
  } catch (e) {
    console.error(e)
    authMessage.value = e?.message || '网络连接错误'
    authError.value = true
  } finally {
    authLoading.value = false
  }
}
/** 退出与登录失效走同一套清理，避免两处漏掉不同的字段。 */
const resetSessionState = () => {
  uploadAbort.value?.abort()
  uploadAbort.value = null
  taskStreams.stopAll()
  resetWorkspace()
  currentUser.value = null
  list.value = []
  searchQuery.value = ''
  videoUrl.value = ''
  file.value = null
  resumableFile.value = null
  resumableChunks.value = { done: 0, total: 0 }
  uploading.value = false
  localStorage.removeItem('user')
}

const logout = () => {
  if (hasAuthToken()) {
    apiRequest('/user/logout', { method: 'POST' }).catch(() => {})
  }
  resetSessionState()
  clearAuthToken()
  showMsg('已退出系统')
}

const handleAuthExpired = () => {
  resetSessionState()
  showMsg('登录状态已失效，请重新登录', true)
  openAuthModal()
}

const handleOnline = () => {
  isOffline.value = false
  showMsg('网络已恢复，正在同步最新状态')
  if (currentUser.value) fetchList()
}

const handleOffline = () => {
  isOffline.value = true
  showMsg('网络已断开：上传会自动重试，后台任务会在恢复后继续', true)
}

// 上传中误关标签页会白丢已传分片，这里让浏览器先问一句。
const handleBeforeUnload = event => {
  if (!uploading.value) return
  event.preventDefault()
  event.returnValue = ''
}

// 打开弹层时挂上标记类，锁背景滚动的规则只在窄屏生效（见样式里的说明）。
const overlayOpen = computed(() => sidebar.value.visible || showAuthModal.value)
watch(overlayOpen, open => {
  document.body.classList.toggle('overlay-open', open)
})

watch(() => sidebar.value.visible, async visible => {
  if (visible) {
    focusBeforeSidebar = document.activeElement
    await nextTick()
    sidebarPanel.value?.focus()
    return
  }
  restoreFocus(focusBeforeSidebar)
  focusBeforeSidebar = null
})

// 长任务给一个时间锚点，用户才不会怀疑是不是卡死了。
watch(() => sidebar.value.loading, loading => {
  clearInterval(elapsedTimer)
  elapsedTimer = null
  elapsedSeconds.value = 0
  if (!loading) return
  const startedAt = Date.now()
  elapsedTimer = setInterval(() => {
    elapsedSeconds.value = Math.floor((Date.now() - startedAt) / 1000)
  }, 1000)
})

onMounted(() => {
  window.addEventListener('auth-expired', handleAuthExpired)
  window.addEventListener('keydown', handleKeydown)
  window.addEventListener('online', handleOnline)
  window.addEventListener('offline', handleOffline)
  window.addEventListener('beforeunload', handleBeforeUnload)
  if (DEMO_MODE) {
    currentUser.value = { id: 1, nickname: 'Agent Demo' }
    list.value = [DEMO_ITEM]
    openAgent(DEMO_ITEM)
    showDemoResult()
    return
  }
  const savedUser = localStorage.getItem('user')
  if (savedUser && hasAuthToken()) {
    try {
      currentUser.value = JSON.parse(savedUser)
    } catch(e) {}
  }
  fetchList({ notify: Boolean(currentUser.value) })
})
onUnmounted(() => {
  window.removeEventListener('auth-expired', handleAuthExpired)
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('online', handleOnline)
  window.removeEventListener('offline', handleOffline)
  window.removeEventListener('beforeunload', handleBeforeUnload)
  clearTimeout(messageTimer)
  clearInterval(elapsedTimer)
  uploadAbort.value?.abort()
  document.body.classList.remove('overlay-open')
  taskStreams.stopAll()
})
</script>

<style>
/* ========== VidMind · 亮色工作台主题（2025-09 重设计） ========== */
.app-stage{
  --bg-page:#f5f6f8; --bg-card:#ffffff; --bg-subtle:#eef1f5; --bg-input:#f9fafb;
  --primary:#4f46e5; --primary-hover:#4338ca; --primary-soft:#eef2ff; --primary-border:#c7d2fe;
  --success:#059669; --success-soft:#ecfdf5; --success-border:#a7f3d0;
  --warning:#b45309; --warning-soft:#fffbeb; --warning-border:#fde68a;
  --danger:#dc2626; --danger-soft:#fef2f2; --danger-border:#fecaca;
  --text-main:#111827; --text-sub:#6b7280; --text-faint:#9ca3af; --text-inverse:#ffffff;
  --border:#e5e7eb; --border-strong:#d1d5db;
  --shadow-sm:0 1px 2px rgba(16,24,40,.06);
  --shadow-md:0 1px 2px rgba(16,24,40,.04),0 12px 28px rgba(16,24,40,.08);
  --shadow-lg:0 24px 64px rgba(16,24,40,.18);
  --radius:12px; --radius-lg:16px; --radius-sm:8px;
  --accent-lime:var(--primary); --accent-purple:#7c3aed; --bg-deep:var(--bg-page);
  --bg-card-old:var(--bg-card); --border-tech:var(--border);
  position:relative; z-index:1; width:100%; min-height:100dvh;
  background:var(--bg-page); color:var(--text-main); font-size:15px; line-height:1.6;
  font-family:'Inter','PingFang SC','HarmonyOS Sans SC','Noto Sans SC','Microsoft YaHei',system-ui,sans-serif;
}
::selection{background:var(--primary-soft);}
.app-stage ::-webkit-scrollbar{width:10px;height:10px;}
.app-stage ::-webkit-scrollbar-thumb{background:#d1d5db;border-radius:8px;border:2px solid var(--bg-page);}
.app-stage ::-webkit-scrollbar-thumb:hover{background:#9ca3af;}

/* 背景特效：亮色主题下移除 */
.ambient-noise,.ambient-glow{display:none;}

/* ---------- 顶栏 ---------- */
.navbar{position:sticky;top:0;z-index:100;width:100%;padding:0;background:rgba(255,255,255,.88);backdrop-filter:blur(10px);border-bottom:1px solid var(--border);}
.nav-content{max-width:1200px;margin:0 auto;padding:14px 24px;display:flex;align-items:center;justify-content:space-between;gap:16px;}
.brand{display:flex;align-items:baseline;gap:1px;font-size:1.3rem;font-weight:800;letter-spacing:-.02em;}
.brand-do{color:var(--primary);font-family:inherit;}
.brand-video{color:var(--text-main);font-family:inherit;font-weight:600;}
.nav-controls{display:flex;align-items:center;gap:12px;}
.user-profile{display:flex;align-items:center;gap:10px;font-size:.9rem;color:var(--text-sub);font-family:inherit;}
.user-name{color:var(--text-main);font-weight:600;}
.status-pill{display:inline-flex;align-items:center;gap:7px;padding:5px 12px;border-radius:999px;font-size:.8rem;font-weight:600;background:var(--bg-subtle);color:var(--text-sub);border:1px solid var(--border);}
.status-pill.is-active{background:var(--primary-soft);color:var(--primary);border-color:var(--primary-border);}
.status-dot{width:8px;height:8px;border-radius:50%;background:var(--text-faint);flex:none;}
.status-pill.is-active .status-dot{background:var(--primary);animation:vm-pulse 1.6s ease-in-out infinite;}
@keyframes vm-pulse{0%,100%{opacity:1}50%{opacity:.3}}

/* ---------- 主体与页头 ---------- */
.main-container{max-width:1200px;margin:0 auto;padding:0 24px 80px;}
.hero-section{padding:52px 0 8px;text-align:center;}
.slogan-main{margin:0;font-size:clamp(1.5rem,3vw,2.3rem);font-weight:800;letter-spacing:-.02em;color:var(--text-main);}
.slogan-sub{margin:10px auto 0;color:var(--text-sub);font-size:1rem;max-width:640px;}

/* ---------- 上传区：白卡 + 双栏 ---------- */
.upload-magnet{position:relative;margin:30px auto 0;max-width:880px;background:var(--bg-card);border:1px solid var(--border);border-radius:var(--radius-lg);box-shadow:var(--shadow-md);padding:22px;transition:border-color .2s,box-shadow .2s;}
.upload-magnet.is-dragover{border-color:var(--primary);box-shadow:0 0 0 4px var(--primary-soft);}
.upload-magnet.processing{border-color:var(--primary-border);}
.split-container{display:grid;grid-template-columns:1fr 1fr;gap:16px;}
.split-gap{display:none;}
.skew-pane{transform:none;cursor:pointer;border:1.5px dashed var(--border-strong);border-radius:var(--radius);background:var(--bg-input);transition:border-color .2s,background .2s,transform .2s;display:block;}
.pane-url{cursor:default;border-style:solid;}
.skew-pane.pane-local:hover{border-color:var(--primary);background:var(--primary-soft);transform:translateY(-1px);}
.pane-content{transform:none;padding:26px 18px;display:flex;flex-direction:column;align-items:center;gap:8px;text-align:center;}
.magnet-icon{width:42px;height:42px;border-radius:10px;background:var(--primary-soft);color:var(--primary);display:flex;align-items:center;justify-content:center;}
.magnet-title{font-weight:700;font-size:1rem;color:var(--text-main);}
.magnet-desc{font-size:.83rem;color:var(--text-sub);}
.url-input-box{display:flex;align-items:center;gap:8px;width:100%;max-width:340px;margin-top:6px;}
.url-input-box input{flex:1;min-width:0;padding:9px 12px;border:1px solid var(--border-strong);border-radius:var(--radius-sm);background:var(--bg-card);color:var(--text-main);font-size:.88rem;transition:border-color .15s,box-shadow .15s;}
.url-input-box input:focus{outline:none;border-color:var(--primary);box-shadow:0 0 0 3px var(--primary-soft);}
.url-go-btn{width:38px;height:38px;flex:none;border:none;border-radius:var(--radius-sm);background:var(--primary);color:#fff;cursor:pointer;display:flex;align-items:center;justify-content:center;transition:background .15s;}
.url-go-btn:hover:not(:disabled){background:var(--primary-hover);}
.url-go-btn:disabled{opacity:.5;cursor:not-allowed;}
.border-glow{display:none;}
.magnet-content.busy{padding:10px 0;display:flex;flex-direction:column;align-items:center;gap:14px;}
.busy-file{font-weight:600;color:var(--text-main);}
.busy-stat{font-size:.88rem;color:var(--text-sub);}
.busy-text{font-size:.88rem;color:var(--text-sub);}
.busy-warning{font-size:.82rem;color:var(--warning);}
.busy-actions{display:flex;gap:10px;}
.upload-progress{margin-top:14px;}
.upload-resume{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-top:12px;padding:12px 14px;border:1px solid var(--border);border-radius:var(--radius-sm);background:var(--bg-input);font-size:.88rem;color:var(--text-sub);}
.upload-resume button{border:1px solid var(--primary-border);background:var(--primary-soft);color:var(--primary);border-radius:var(--radius-sm);padding:7px 14px;font-weight:600;cursor:pointer;transition:background .15s;}
.upload-resume button:hover{background:#e0e7ff;}

/* ---------- 加载指示 ---------- */
.quantum-loader{width:44px;height:44px;border-radius:50%;border:3px solid var(--primary-soft);border-top-color:var(--primary);animation:vm-spin 1s linear infinite;margin:0 auto;}
.quantum-loader.small{width:20px;height:20px;border-width:2px;display:inline-block;vertical-align:middle;}
@keyframes vm-spin{to{transform:rotate(360deg)}}
.loading-state{display:flex;flex-direction:column;align-items:center;gap:12px;padding:18px 0;color:var(--text-sub);font-size:.9rem;}
.loading-hint{color:var(--text-faint);font-size:.82rem;}

/* ---------- 资料库 ---------- */
.workspace-section{margin-top:44px;}
.section-header{display:flex;align-items:center;justify-content:space-between;gap:14px;margin-bottom:16px;flex-wrap:wrap;}
.library-title{display:flex;align-items:center;gap:10px;}
.library-title h3{margin:0;font-size:1.08rem;font-weight:700;}
.count-chip{padding:3px 10px;border-radius:999px;background:var(--primary-soft);color:var(--primary);font-size:.78rem;font-weight:600;}
.beta-badge{padding:2px 8px;border-radius:999px;background:var(--bg-subtle);color:var(--text-faint);font-size:.7rem;border:1px solid var(--border);}
.library-search{width:min(320px,42vw);display:flex;align-items:center;gap:8px;padding:8px 12px;border:1px solid var(--border);border-radius:10px;background:var(--bg-card);color:var(--text-faint);transition:border-color .15s,box-shadow .15s;}
.library-search:focus-within{border-color:var(--primary);box-shadow:0 0 0 3px var(--primary-soft);}
.library-search input{border:none;outline:none;background:transparent;flex:1;min-width:0;color:var(--text-main);font-size:.9rem;}
.card-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(340px,1fr));gap:16px;}
.project-card{background:var(--bg-card);border:1px solid var(--border);border-radius:var(--radius);box-shadow:var(--shadow-sm);overflow:hidden;transition:box-shadow .2s,transform .2s,border-color .2s;}
.project-card:hover{box-shadow:var(--shadow-md);transform:translateY(-2px);border-color:var(--border-strong);}
.card-meta{display:flex;gap:14px;padding:16px;align-items:flex-start;background:var(--bg-card);border-bottom:1px solid var(--border);}
.meta-icon{width:46px;height:46px;flex:none;background:var(--primary-soft);border:1px solid var(--primary-border);border-radius:10px;display:flex;align-items:center;justify-content:center;color:var(--primary);}
.meta-info{display:flex;flex-direction:column;gap:6px;min-width:0;flex:1;}
.meta-label{font-weight:600;font-size:.95rem;color:var(--text-main);}
.filename-mask{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}
.meta-tags{display:flex;flex-wrap:wrap;gap:6px;align-items:center;}
.time-tag{padding:2px 8px;border-radius:6px;background:var(--bg-subtle);color:var(--text-sub);font-size:.75rem;font-family:ui-monospace,Consolas,monospace;}
.status-indicator{display:inline-flex;align-items:center;gap:6px;padding:3px 10px;border-radius:999px;font-size:.76rem;font-weight:600;border:1px solid var(--border);background:var(--bg-subtle);color:var(--text-sub);}
.status-indicator.completed{color:var(--success);background:var(--success-soft);border-color:var(--success-border);}
.status-indicator.processing{color:var(--warning);background:var(--warning-soft);border-color:var(--warning-border);}
.status-indicator.error{color:var(--danger);background:var(--danger-soft);border-color:var(--danger-border);}
.action-dock{display:flex;align-items:center;justify-content:space-between;gap:10px;padding:11px 16px;flex-wrap:wrap;}
.label-group{display:flex;align-items:center;gap:10px;min-width:0;}
.item-icon,.item-label{font-size:.85rem;color:var(--text-sub);}
.dock-item{display:inline-flex;align-items:center;gap:6px;padding:7px 13px;border-radius:var(--radius-sm);border:1px solid var(--border);background:var(--bg-card);color:var(--text-main);font-size:.85rem;font-weight:500;cursor:pointer;transition:all .15s;}
.dock-item:hover:not(:disabled){color:var(--primary);border-color:var(--primary-border);background:var(--primary-soft);}
.dock-item:disabled{opacity:.45;cursor:not-allowed;}
.dock-item.active{color:var(--primary);border-color:var(--primary);background:var(--primary-soft);font-weight:600;}
.delete-btn{border:none;background:transparent;color:var(--text-faint);cursor:pointer;border-radius:8px;padding:6px;display:flex;align-items:center;transition:all .15s;}
.delete-btn:hover{color:var(--danger);background:var(--danger-soft);}
.btn-icon{border:none;background:transparent;color:var(--text-sub);cursor:pointer;padding:4px;border-radius:6px;display:inline-flex;}
.btn-icon:hover{color:var(--primary);background:var(--primary-soft);}
.shimmer{position:relative;overflow:hidden;background:var(--bg-subtle);border-radius:8px;}
.shimmer::after{content:'';position:absolute;inset:0;transform:translateX(-100%);background:linear-gradient(90deg,transparent,rgba(255,255,255,.65),transparent);animation:vm-shimmer 1.4s infinite;}
@keyframes vm-shimmer{to{transform:translateX(100%)}}
.library-empty{border:1.5px dashed var(--border-strong);border-radius:var(--radius-lg);padding:52px 20px;text-align:center;color:var(--text-sub);background:var(--bg-card);}
.text-meta{color:var(--text-sub);font-size:.82rem;}
.messageIsError{color:var(--danger);}

/* ---------- 通知条 ---------- */
.notification-bar{margin-top:1.6rem;display:inline-block;background:var(--primary-soft);color:var(--primary);padding:10px 20px;font-weight:600;font-size:.9rem;border-radius:10px;border:1px solid var(--primary-border);}
.notification-bar.error{background:var(--danger-soft);color:var(--danger);border-color:var(--danger-border);cursor:pointer;}

/* ---------- 右侧抽屉：Agent 面板 ---------- */
.sidebar-backdrop{position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(17,24,39,.45);z-index:998;}
.sidebar-panel{position:fixed;top:0;right:-920px;width:880px;max-width:calc(100vw - 24px);height:100dvh;background:var(--bg-card);z-index:999;transition:right .35s cubic-bezier(.32,.72,0,1);display:flex;flex-direction:column;box-shadow:var(--shadow-lg);border-radius:var(--radius-lg) 0 0 var(--radius-lg);overflow:hidden;}
.sidebar-panel.is-open{right:0;}
.sidebar-header{padding:18px 24px;border-bottom:1px solid var(--border);display:flex;justify-content:space-between;align-items:center;background:var(--bg-card);}
.sidebar-title{font-weight:700;font-size:1rem;display:flex;align-items:center;gap:10px;}
.sidebar-body{flex:1;overflow-y:auto;padding:20px 24px 32px;}
.close-btn{border:none;background:var(--bg-subtle);color:var(--text-sub);width:32px;height:32px;border-radius:8px;font-size:1.1rem;cursor:pointer;display:flex;align-items:center;justify-content:center;transition:all .15s;}
.close-btn:hover{background:var(--danger-soft);color:var(--danger);}

.video-evidence{margin:-20px -24px 20px;background:var(--bg-subtle);border-bottom:1px solid var(--border);padding:14px 24px;}
.video-evidence-loading,.video-evidence-error{display:flex;align-items:center;gap:10px;font-size:.88rem;color:var(--text-sub);padding:6px 0;}
.video-evidence-error{color:var(--danger);}
.ai-core{display:flex;flex-direction:column;gap:16px;}

.agent-composer{display:flex;flex-direction:column;gap:12px;}
.agent-mode-row{display:flex;gap:8px;flex-wrap:wrap;}
.goal-presets{display:flex;flex-wrap:wrap;gap:8px;}
.goal-presets button{padding:6px 12px;border-radius:999px;border:1px solid var(--border);background:var(--bg-card);color:var(--text-sub);font-size:.82rem;cursor:pointer;transition:all .15s;}
.goal-presets button:hover{color:var(--primary);border-color:var(--primary-border);background:var(--primary-soft);}
.goal-presets button.active{color:var(--primary);border-color:var(--primary);background:var(--primary-soft);box-shadow:0 0 0 3px var(--primary-soft);font-weight:600;}
.goal-presets button.active strong{color:var(--primary);}
.goal-presets button.active span{color:var(--text-sub);}
.agent-running{display:flex;flex-direction:column;gap:14px;}
.agent-caption{font-size:.85rem;font-weight:600;color:var(--text-sub);text-transform:none;letter-spacing:.02em;margin:4px 0 0;}
.agent-run-btn,.cyber-btn{display:inline-flex;align-items:center;justify-content:center;gap:8px;padding:10px 18px;border:none;border-radius:10px;background:var(--primary);color:#fff;font-weight:600;font-size:.92rem;cursor:pointer;transition:background .15s,transform .1s;font-family:inherit;}
.agent-run-btn:hover:not(:disabled),.cyber-btn:hover:not(:disabled){background:var(--primary-hover);}
.agent-run-btn:disabled,.cyber-btn:disabled{opacity:.55;cursor:not-allowed;}
.agent-run-btn:active:not(:disabled),.cyber-btn:active:not(:disabled){transform:scale(.98);}

.agent-meta-block{background:var(--bg-subtle);border:1px solid var(--border);border-left:3px solid var(--primary);border-radius:10px;padding:14px 16px;margin-bottom:16px;}
.stage-list{display:flex;flex-direction:column;gap:8px;margin:0;padding:0;list-style:none;}
.stage-list > *{background:var(--bg-card);border:1px solid var(--border);border-radius:10px;padding:10px 12px;font-size:.88rem;color:var(--text-main);}
.result-actions{display:flex;flex-wrap:wrap;gap:10px;margin:14px 0;}
.result-actions button{padding:8px 14px;border-radius:var(--radius-sm);border:1px solid var(--border);background:var(--bg-card);color:var(--text-main);font-size:.85rem;font-weight:500;cursor:pointer;transition:all .15s;font-family:inherit;}
.result-actions button:hover{color:var(--primary);border-color:var(--primary-border);background:var(--primary-soft);}
.evidence-search{display:flex;flex-direction:column;gap:10px;}
.evidence-search-form{display:flex;gap:8px;}
.evidence-search-form input{flex:1;min-width:0;padding:9px 12px;border:1px solid var(--border-strong);border-radius:var(--radius-sm);background:var(--bg-input);color:var(--text-main);font-size:.88rem;}
.evidence-search-form input:focus{outline:none;border-color:var(--primary);box-shadow:0 0 0 3px var(--primary-soft);}
.evidence-search-results{display:flex;flex-direction:column;gap:8px;font-size:.88rem;}
.evidence-search-error{color:var(--danger);font-size:.85rem;}
.markdown-content{font-size:.92rem;color:var(--text-main);line-height:1.75;}
.markdown-content h1,.markdown-content h2,.markdown-content h3,.markdown-content h4{margin:18px 0 8px;line-height:1.4;color:var(--text-main);}
.markdown-content h1{font-size:1.2rem;}.markdown-content h2{font-size:1.08rem;}.markdown-content h3{font-size:1rem;}
.markdown-content p{margin:8px 0;}
.markdown-content ul,.markdown-content ol{margin:8px 0;padding-left:22px;}
.markdown-content li{margin:4px 0;}
.markdown-content code{background:var(--bg-subtle);border:1px solid var(--border);border-radius:6px;padding:1px 6px;font-size:.85em;font-family:ui-monospace,Consolas,monospace;}
.markdown-content strong{font-weight:700;}
.agent-inspector,.agent-inspector-content{display:flex;flex-direction:column;gap:14px;}
.plan-editor{display:flex;flex-direction:column;gap:10px;}
.plan-editor-row{display:flex;gap:8px;align-items:center;}
.plan-editor input,.plan-editor textarea{min-width:0;flex:1;border:1px solid var(--border-strong);border-radius:var(--radius-sm);background:var(--bg-input);color:var(--text-main);padding:9px 10px;font-size:.88rem;font-family:inherit;}
.plan-editor input:focus,.plan-editor textarea:focus{outline:none;border-color:var(--primary);box-shadow:0 0 0 3px var(--primary-soft);}
.plan-editor-actions{display:flex;gap:8px;flex-wrap:wrap;}
.plan-edit-trigger{border:none;background:transparent;color:var(--primary);cursor:pointer;font-size:.85rem;padding:4px 0;font-weight:600;}
.plan-edit-trigger:hover{text-decoration:underline;}
.quality-row{display:flex;flex-wrap:wrap;gap:8px;}
.quality-row > *{padding:4px 10px;border-radius:999px;background:var(--bg-subtle);border:1px solid var(--border);font-size:.78rem;color:var(--text-sub);}
.follow-up-box{display:flex;flex-direction:column;gap:10px;}
.follow-up-box textarea{width:100%;min-height:80px;resize:vertical;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-input);color:var(--text-main);padding:11px 12px;font-size:.9rem;font-family:inherit;transition:border-color .15s,box-shadow .15s;}
.follow-up-box textarea:focus{outline:none;border-color:var(--primary);box-shadow:0 0 0 3px var(--primary-soft);}
.feedback-row{display:flex;gap:10px;}
.feedback-row button{display:inline-flex;align-items:center;gap:6px;padding:7px 16px;border-radius:999px;border:1px solid var(--border);background:var(--bg-card);color:var(--text-sub);font-size:.85rem;cursor:pointer;transition:all .15s;}
.feedback-row button:hover{border-color:var(--primary-border);color:var(--primary);}
.feedback-row button.active{background:var(--primary-soft);border-color:var(--primary);color:var(--primary);font-weight:600;}
/* Agent 抽屉内其余按钮组的交互反馈（重写主题时补齐） */
.busy-actions button{border:1px solid var(--border);background:var(--bg-card);color:var(--text-sub);padding:7px 14px;border-radius:var(--radius-sm);font-size:.82rem;cursor:pointer;transition:all .15s;font-family:inherit;}
.busy-actions button:hover{color:var(--primary);border-color:var(--primary-border);background:var(--primary-soft);}
.evidence-search-form button,.evidence-search-results button{border:1px solid var(--border);background:var(--bg-card);color:var(--text-sub);border-radius:var(--radius-sm);cursor:pointer;transition:all .15s;font-family:inherit;}
.evidence-search-form button:hover,.evidence-search-results button:hover{color:var(--primary);border-color:var(--primary-border);background:var(--primary-soft);}
.evidence-search-results button{display:grid;grid-template-columns:58px 70px minmax(0,1fr);gap:10px;text-align:left;padding:8px 10px;font-size:.85rem;}
.follow-up-box button{border:1px solid var(--border);background:var(--bg-card);color:var(--text-sub);border-radius:999px;padding:7px 16px;cursor:pointer;transition:all .15s;font-family:inherit;}
.follow-up-box button:hover{color:var(--primary);border-color:var(--primary-border);background:var(--primary-soft);}
.plan-editor button{border:1px solid var(--border);background:var(--bg-card);color:var(--text-sub);padding:7px 10px;border-radius:var(--radius-sm);cursor:pointer;transition:all .15s;font-family:inherit;}
.plan-editor button:hover{color:var(--primary);border-color:var(--primary-border);background:var(--primary-soft);}
.text-content{background:var(--bg-card);border:1px solid var(--border);border-radius:10px;padding:16px;font-size:.92rem;}
.text-content pre{white-space:pre-wrap;font-family:ui-monospace,Consolas,monospace;background:var(--bg-subtle);padding:14px;border-radius:8px;border:1px solid var(--border);color:#334155;font-size:.85rem;}

/* ---------- 登录 / 注册弹窗 ---------- */
.auth-backdrop{position:fixed;inset:0;background:rgba(17,24,39,.45);backdrop-filter:blur(3px);z-index:1000;display:flex;align-items:center;justify-content:center;padding:16px;}
.auth-panel{width:420px;max-width:100%;background:var(--bg-card);border-radius:var(--radius-lg);box-shadow:var(--shadow-lg);overflow:hidden;}
.auth-header{padding:24px 26px 0;}
.auth-title{margin:0;font-size:1.15rem;font-weight:700;color:var(--text-main);}
.auth-body{padding:20px 26px 26px;display:flex;flex-direction:column;gap:14px;}
.input-group{display:flex;flex-direction:column;gap:6px;}
.input-group input{width:100%;padding:10px 12px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-input);color:var(--text-main);font-size:.92rem;transition:border-color .15s,box-shadow .15s;}
.input-group input:focus{outline:none;border-color:var(--primary);box-shadow:0 0 0 3px var(--primary-soft);}
.field-counter{font-size:.75rem;color:var(--text-faint);text-align:right;}
.inline-error{color:var(--danger);font-size:.8rem;}
.auth-msg{font-size:.85rem;color:var(--text-sub);min-height:1em;}
.auth-msg.authError{color:var(--danger);}
.auth-action{display:flex;flex-direction:column;gap:10px;}
.auth-btn{background:transparent;border:1px solid var(--primary-border);color:var(--primary);padding:7px 16px;border-radius:10px;font-weight:600;cursor:pointer;display:inline-flex;align-items:center;gap:8px;transition:all .15s;font-size:.85rem;font-family:inherit;}
.auth-btn:hover{background:var(--primary-soft);border-color:var(--primary);}
.logout-btn{border:none;background:transparent;color:var(--text-sub);cursor:pointer;font-size:.85rem;padding:6px 10px;border-radius:8px;font-family:inherit;transition:all .15s;}
.logout-btn:hover{color:var(--danger);background:var(--danger-soft);}
.toggle-link{background:none;border:none;color:var(--primary);cursor:pointer;font-size:.85rem;padding:0;font-family:inherit;}
.toggle-link:hover{text-decoration:underline;}
.toggle-text{color:var(--text-sub);}

/* ---------- 响应式 ---------- */
@media (max-width:860px){
  .split-container{grid-template-columns:1fr;}
  .main-container{padding:0 16px 60px;}
  .hero-section{padding:32px 0 4px;}
  .sidebar-panel{width:100vw;max-width:100vw;right:-100vw;border-radius:0;}
  .card-grid{grid-template-columns:1fr;}
  .nav-content{padding:12px 16px;}
  .video-evidence{margin:-16px -16px 16px;padding:12px 16px;}
}
@media (prefers-reduced-motion:reduce){
  *,*::before,*::after{animation-duration:.01ms !important;animation-iteration-count:1 !important;transition-duration:.01ms !important;}
}

/* ---------- 视觉补强：氛围光斑 / 引导条 / 页脚 ---------- */
.hero-section{position:relative;}
.hero-section::before{content:'';position:absolute;top:-140px;left:50%;transform:translateX(-50%);width:760px;height:340px;background:radial-gradient(closest-side,rgba(79,70,229,.10),transparent 70%);pointer-events:none;}
.slogan-main,.slogan-sub,.login-hint{position:relative;}
.login-hint{margin:18px auto 0;display:inline-flex;align-items:center;gap:10px;padding:8px 16px;border-radius:999px;background:var(--primary-soft);border:1px solid var(--primary-border);font-size:.88rem;color:var(--text-sub);}
.login-hint button{background:none;border:none;color:var(--primary);font-weight:600;font-size:.88rem;cursor:pointer;padding:0;font-family:inherit;}
.login-hint button:hover{text-decoration:underline;}
.app-footer{margin-top:72px;padding:22px 0 0;border-top:1px solid var(--border);display:flex;align-items:center;justify-content:center;gap:10px;font-size:.82rem;color:var(--text-faint);}
.footer-dot{color:var(--border-strong);}
.url-input-box{max-width:100%;}
.pane-content{padding:24px 16px;}
.library-empty .loading-hint{margin-top:6px;}
</style>
