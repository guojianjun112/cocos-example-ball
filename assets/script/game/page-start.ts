import {
    _decorator,
    Button,
    Color,
    Component,
    EditBox,
    EventTouch,
    Label,
    Node,
    Sprite,
    UITransform,
    sys,
} from "cc";
import { Constants } from "../data/constants";
const { ccclass } = _decorator;

@ccclass("PageStart")
export class PageStart extends Component {
    private static readonly BASE_URL = "https://www.wxzj.online";
    private static readonly API_LOGIN_GETCODE = "/api/Login/sendSms";
    private static readonly API_LOGIN_CODE = "/api/Login/loginSms";
    private static readonly XIEYI_URL = "https://www.wxzj.online/bqxxl_user.html";
    private static readonly YINSI_URL = "https://www.wxzj.online/bqxxl_privacy.html";
    private static readonly USER_STORAGE_KEY = "bqxxl_user";

    private loginMask: Node | null = null;
    private loginPanel: Node | null = null;
    private phoneEdit: EditBox | null = null;
    private codeEdit: EditBox | null = null;
    private agreeDot: Node | null = null;
    private tipLabel: Label | null = null;
    private agreed = true;

    start() {
        const user = this.getStoredUser();
        if (!user?.token) {
            this.showLoginPanel();
        }
    }

    gameStart() {
        const user = this.getStoredUser();
        if (!user?.token) {
            this.showLoginPanel();
            return;
        }
        this.startGame();
    }

    private startGame() {
        Constants.game.node.emit(Constants.GAME_EVENT.RESTART);
        Constants.game.audioManager.playClip();
    }

    private showLoginPanel() {
        if (this.loginMask) {
            this.loginMask.active = true;
            return;
        }

        this.loginMask = this.createNode("LoginMask", this.node, 0, 0, 720, 1280);
        this.addSprite(this.loginMask, new Color(0, 0, 0, 180));

        this.loginPanel = this.createNode("LoginPanel", this.loginMask, 0, 60, 620, 580);
        this.addSprite(this.loginPanel, new Color(252, 241, 220, 255));

        const titleBg = this.createNode("TitleBg", this.loginPanel, 0, 235, 320, 88);
        this.addSprite(titleBg, new Color(233, 135, 67, 255));
        this.addLabel(titleBg, "登录", 52, new Color(255, 255, 255, 255));

        const closeBtn = this.createButton(this.loginPanel, "X", 275, 235, 62, 62, new Color(232, 133, 66, 255), new Color(255, 255, 255, 255), 42);
        closeBtn.on(Node.EventType.TOUCH_END, this.onCloseLogin, this);

        this.addRow("手机号", 120, "请输入您的手机号", true);
        this.addRow("验证码", 30, "请输入您的验证码", false);

        const getCodeBtn = this.createButton(this.loginPanel, "获取验证码", 220, 30, 120, 58, new Color(138, 217, 58, 255), new Color(255, 255, 255, 255), 34);
        getCodeBtn.on(Node.EventType.TOUCH_END, this.onGetCode, this);

        const agreeWrap = this.createNode("AgreeWrap", this.loginPanel, 0, -80, 520, 56);
        this.addSprite(agreeWrap, new Color(248, 225, 170, 255));

        this.agreeDot = this.createNode("AgreeDot", agreeWrap, -230, 0, 24, 24);
        this.addSprite(this.agreeDot, new Color(95, 55, 15, 255));
        this.agreeDot.active = this.agreed;

        const agreeBtn = this.createNode("AgreeBtn", agreeWrap, -230, 0, 40, 40);
        agreeBtn.on(Node.EventType.TOUCH_END, this.onToggleAgree, this);

        this.addLabel(agreeWrap, "已阅读并同意", 28, new Color(92, 56, 21, 255), -130, 0);

        const xieyiBtn = this.createNode("XieyiBtn", agreeWrap, 35, 0, 140, 40);
        this.addLabel(xieyiBtn, "《用户协议》", 28, new Color(74, 184, 74, 255));
        xieyiBtn.on(Node.EventType.TOUCH_END, () => sys.openURL(PageStart.XIEYI_URL), this);

        this.addLabel(agreeWrap, "与", 28, new Color(92, 56, 21, 255), 110, 0);

        const yinsiBtn = this.createNode("YinsiBtn", agreeWrap, 190, 0, 160, 40);
        this.addLabel(yinsiBtn, "《隐私政策》", 28, new Color(74, 184, 74, 255));
        yinsiBtn.on(Node.EventType.TOUCH_END, () => sys.openURL(PageStart.YINSI_URL), this);

        const loginBtn = this.createButton(this.loginPanel, "登录", 0, -185, 260, 92, new Color(255, 211, 0, 255), new Color(148, 88, 10, 255), 58);
        loginBtn.on(Node.EventType.TOUCH_END, this.onLogin, this);

        const tipNode = this.createNode("TipLabel", this.loginPanel, 0, -250, 540, 36);
        this.tipLabel = this.addLabel(tipNode, "", 24, new Color(200, 70, 50, 255));
    }

    private addRow(label: string, y: number, placeholder: string, isPhone: boolean) {
        const rowLabel = this.createNode(`${label}Label`, this.loginPanel!, -235, y, 120, 48);
        this.addLabel(rowLabel, label, 42, new Color(75, 44, 24, 255));

        const inputBg = this.createNode(`${label}InputBg`, this.loginPanel!, 35, y, 420, 74);
        this.addSprite(inputBg, new Color(245, 219, 149, 255));

        const inputNode = this.createNode(`${label}Input`, inputBg, 0, 0, 390, 58);
        const edit = inputNode.addComponent(EditBox);
        edit.string = "";
        edit.placeholder = placeholder;
        edit.maxLength = isPhone ? 11 : 6;
        edit.inputMode = isPhone ? EditBox.InputMode.PHONE_NUMBER : EditBox.InputMode.NUMERIC;
        edit.returnType = EditBox.KeyboardReturnType.DONE;
        edit.placeholderLabel = this.addLabel(inputNode, placeholder, 32, new Color(211, 126, 72, 255));
        edit.textLabel = this.addLabel(inputNode, "", 32, new Color(95, 56, 20, 255));

        if (isPhone) {
            this.phoneEdit = edit;
        } else {
            this.codeEdit = edit;
        }
    }

    private createNode(name: string, parent: Node, x: number, y: number, w: number, h: number) {
        const n = new Node(name);
        n.setParent(parent);
        const trans = n.addComponent(UITransform);
        trans.setContentSize(w, h);
        n.setPosition(x, y);
        return n;
    }

    private addSprite(node: Node, color: Color) {
        const sp = node.addComponent(Sprite);
        sp.color = color;
        return sp;
    }

    private addLabel(node: Node, text: string, fontSize: number, color: Color, x = 0, y = 0) {
        const labelNode = new Node(`${node.name}_Label`);
        labelNode.setParent(node);
        const trans = labelNode.addComponent(UITransform);
        trans.setContentSize(node.getComponent(UITransform)!.contentSize);
        labelNode.setPosition(x, y);
        const label = labelNode.addComponent(Label);
        label.string = text;
        label.fontSize = fontSize;
        label.color = color;
        label.lineHeight = fontSize + 6;
        label.overflow = Label.Overflow.SHRINK;
        return label;
    }

    private createButton(parent: Node, text: string, x: number, y: number, w: number, h: number, bg: Color, fg: Color, fontSize: number) {
        const btnNode = this.createNode(`${text}Btn`, parent, x, y, w, h);
        this.addSprite(btnNode, bg);
        btnNode.addComponent(Button);
        this.addLabel(btnNode, text, fontSize, fg);
        return btnNode;
    }

    private onCloseLogin() {
        if (this.loginMask) {
            this.loginMask.active = false;
        }
    }

    private onToggleAgree(_evt?: EventTouch) {
        this.agreed = !this.agreed;
        if (this.agreeDot) {
            this.agreeDot.active = this.agreed;
        }
    }

    private showTip(text: string) {
        if (this.tipLabel) {
            this.tipLabel.string = text;
        }
        console.log("LoginTip:", text);
    }

    private async onGetCode() {
        const phone = this.phoneEdit?.string?.trim() || "";
        if (!/^1\d{10}$/.test(phone)) {
            this.showTip("请输入正确的手机号");
            return;
        }

        const resp = await this.post(PageStart.API_LOGIN_GETCODE, { phone, type: 1 }, false);
        if (resp?.code === 1) {
            this.showTip(resp.msg || "验证码发送成功");
        } else {
            this.showTip(resp?.msg || "验证码获取失败");
        }
    }

    private async onLogin() {
        const phone = this.phoneEdit?.string?.trim() || "";
        const code = this.codeEdit?.string?.trim() || "";
        if (!/^1\d{10}$/.test(phone)) {
            this.showTip("请输入正确的手机号");
            return;
        }
        if (!/^\d{4,6}$/.test(code)) {
            this.showTip("请输入正确的验证码");
            return;
        }

        const resp = await this.post(PageStart.API_LOGIN_CODE, { phone, code }, false);
        if (resp?.code === 1 && resp?.data?.token) {
            const user = this.getStoredUser() || {};
            user.phone = phone;
            user.playerInfo = resp.data;
            user.token = resp.data.token;
            sys.localStorage.setItem(PageStart.USER_STORAGE_KEY, JSON.stringify(user));
            this.showTip("登录成功");
            if (this.loginMask) {
                this.loginMask.active = false;
            }
            this.startGame();
            return;
        }
        this.showTip(resp?.msg || "登录失败");
    }

    private getStoredUser() {
        try {
            return JSON.parse(sys.localStorage.getItem(PageStart.USER_STORAGE_KEY) || "{}");
        } catch (e) {
            return {};
        }
    }

    private getToken() {
        const user = this.getStoredUser();
        return user?.token || "";
    }

    private async post(path: string, body: any, authen = true) {
        const headers: any = {
            "Content-Type": "application/json",
        };
        if (authen) {
            const token = this.getToken();
            if (token) headers.Authorization = token;
        }

        try {
            const response = await fetch(`${PageStart.BASE_URL}${path}`, {
                method: "POST",
                headers,
                body: JSON.stringify(body),
            });
            if (!response.ok) {
                return { code: -1, msg: `HTTP ${response.status}` };
            }
            return await response.json();
        } catch (e: any) {
            return { code: -1, msg: e?.message || "网络异常" };
        }
    }
}

